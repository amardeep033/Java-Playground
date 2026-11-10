"""Isolated full-stack verification. Requires Node 22+, Java 21, Maven, Docker, psql."""
from concurrent.futures import ThreadPoolExecutor
import http.cookiejar
import json
import importlib.util
import sys
sys.dont_write_bytecode = True
BROWSER_ONLY = "--browser-only" in sys.argv
if BROWSER_ONLY: sys.argv.remove("--browser-only")
import os
from pathlib import Path
import socket
import subprocess
import tempfile
import threading
import time
import urllib.request
import urllib.error
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

ROOT = Path(__file__).resolve().parents[2]
RUN = uuid.uuid4().hex
DATABASE = 'event_relay_test_' + RUN[:12]
BROKER = 'event-relay-ui-test-' + RUN[:12]
ARTIFACTS = Path(tempfile.mkdtemp(prefix='event-relay-ui-test-'))
processes = []
logfiles = []
database_created = False
calls = []
statuses = {'/webhook': 503, '/api-check': 503}
lock = threading.Lock()
checks = 0

def free_port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        return sock.getsockname()[1]

class Receiver(BaseHTTPRequestHandler):
    def log_message(self, *_): pass
    def do_POST(self):
        raw = self.rfile.read(int(self.headers.get('Content-Length', 0)))
        if self.path == '/control':
            value = json.loads(raw)
            with lock: statuses[value['path']] = value['status']
            body = b'{}'
            code = 200
        else:
            with lock:
                code = statuses.get(self.path, 200)
                calls.append((self.path, raw, dict(self.headers)))
            body = b'x' * 7000 if self.path == '/large' else json.dumps({'message': 'temporary failure' if code >= 400 else 'accepted'}).encode()
        self.send_response(code)
        self.send_header('Content-Type', 'text/plain' if self.path == '/large' else 'application/json')
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        if self.path == '/stream': time.sleep(.5)
        try: self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError): pass

receiver = ThreadingHTTPServer(('127.0.0.1', 0), Receiver)
receiver.daemon_threads = True
threading.Thread(target=receiver.serve_forever, daemon=True).start()
receiver_url = f'http://127.0.0.1:{receiver.server_port}'
port_a, port_b, broker_port, ui_port = free_port(), free_port(), free_port(), free_port()
base = f'http://127.0.0.1:{port_a}/api/v1'

def sql(query, database=DATABASE):
    result = subprocess.run(['psql', '-h', 'localhost', '-U', 'postgres', '-d', database, '-tA', '-v', 'ON_ERROR_STOP=1', '-c', query],
                            env=dict(os.environ, PGPASSWORD=os.environ.get('PGPASSWORD', 'postgres')), text=True, capture_output=True, check=True)
    return result.stdout.strip()

cookie_jar = http.cookiejar.CookieJar()
browser = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cookie_jar))
def api(method, path, body=None, expected=200, headers=None, anonymous=False):
    global checks
    if method=='POST' and path=='/users' and body is not None: body={'companyName':'Test company',**body}
    extra = dict(headers or {})
    use_key = 'X-API-Key' in extra
    if method not in ('GET','HEAD') and not use_key and not anonymous:
        csrf = api('GET','/auth/csrf')
        extra[csrf['headerName']] = csrf['token']
    request = urllib.request.Request(base + path, method=method, data=None if body is None else json.dumps(body).encode(), headers={'Content-Type': 'application/json', **extra})
    try: response = (urllib.request.urlopen if use_key or anonymous else browser.open)(request, timeout=10)
    except urllib.error.HTTPError as error: response = error
    with response:
        raw = response.read()
        assert response.status == expected, (method, path, response.status, raw)
        checks += 1
        return json.loads(raw) if raw else None

def wait_for(operation, predicate, timeout=25):
    until = time.monotonic() + timeout
    last = None
    while time.monotonic() < until:
        last = operation()
        if predicate(last): return last
        time.sleep(.2)
    raise AssertionError(('Timed out', last))

def start(folder, artifact, port):
    log = open(ARTIFACTS / (artifact + '.log'), 'w+')
    logfiles.append(log)
    process = subprocess.Popen(['java', '-jar', str(ROOT / folder / 'target' / (artifact + '-0.0.1-SNAPSHOT.jar')),
            f'--server.port={port}', f'--spring.rabbitmq.port={broker_port}', '--app.retry-delay-ms=300', '--app.outbox.poll-delay-ms=100',
            f'--spring.datasource.url=jdbc:postgresql://localhost:5432/{DATABASE}', f'--app.signing.key-file={ARTIFACTS}/signing.key', f'--app.auth.key-file={ARTIFACTS}/auth.key',f'--app.internal.key-file={ARTIFACTS}/internal.key',f'--app.reconciliation.service-b-url=http://127.0.0.1:{port_b}','--app.reconciliation.poll-delay-ms=1000','--app.reconciliation.initial-delay-ms=1000'],
            cwd=ROOT/folder, stdout=log, stderr=subprocess.STDOUT)
    processes.append(process)
    for _ in range(180):
        log.seek(0); text = log.read()
        if process.poll() is not None: raise RuntimeError(text[-8000:])
        if ('Started ServiceAApplication' if artifact == 'service-a' else 'Started ServiceBApplication') in text: return
        time.sleep(.2)
    raise RuntimeError('Service startup timed out: ' + text[-6000:])

def verify_backend():
    user = api('POST', '/users', {'name': 'Monitoring checks', 'email': f'checks-{RUN}@example.com', 'password': 'ExamplePass123!'}, 201)
    app = api('POST', f'/users/{user["id"]}/applications', {'name': 'Monitoring checks'}, 201)['id']
    prefix = f'/applications/{app}'
    key = api('POST', prefix+'/api-keys', {'name': 'Checks'}, 201)['secret']
    headers = {'X-API-Key': key}
    kind = api('POST', prefix+'/event-types', {'name': 'monitoring.check'}, 201)['id']
    issued = api('POST', prefix+'/endpoints', {'url': receiver_url+'/api-check', 'eventTypeIds': [kind], 'configuration': {'enabled': True, 'rateLimitPerSecond': 10, 'concurrencyLimit': 4, 'timeoutMs': 2000, 'maxAttempts': 2}}, 201)
    endpoint = issued['endpoint']
    receipt = api('POST', prefix+'/events', {'eventType': 'monitoring.check', 'payload': {'orderId': 'checks'}}, 202, {**headers, 'Idempotency-Key': 'first'})
    detail = wait_for(lambda: api('GET', prefix+'/events/'+receipt['id'], headers=headers), lambda data: data['status'] == 'FAILED')
    delivery = detail['deliveries'][0]['id']
    original = api('GET', prefix+'/deliveries/'+delivery, headers=headers)
    assert len(original['attempts']) == 2 and 'temporary failure' in original['attempts'][-1]['responseBody']
    assert issued['signingSecret'] not in json.dumps(original)
    assert original['delivery']['canReplay']
    api('GET', prefix+'/events', expected=401, anonymous=True)
    api('GET', prefix+'/events?page=-1', expected=400, headers=headers)
    api('GET', prefix+'/deliveries?size=101', expected=400, headers=headers)
    api('GET', prefix+'/deliveries?status=UNKNOWN', expected=400, headers=headers)
    other = api('POST', f'/users/{user["id"]}/applications', {'name': 'Other app'}, 201)['id']
    api('GET', f'/applications/{other}/deliveries', expected=403, headers=headers)
    api('POST', prefix+'/deliveries/'+delivery+'/replay', expected=400, headers=headers)
    api('PUT', prefix+'/endpoints/'+endpoint['id']+'/admin', {'active':False})
    assert not api('GET', prefix+'/deliveries/'+delivery, headers=headers)['delivery']['canReplay']
    api('POST', prefix+'/deliveries/'+delivery+'/replay', expected=409, headers={**headers, 'Idempotency-Key': 'disabled'})
    api('PUT', prefix+'/endpoints/'+endpoint['id']+'/admin', {'active':True})
    # App/event-type pauses also disable new replays, without hiding history.
    for resource in [f'/users/{user["id"]}/applications/{app}', prefix+'/event-types/'+kind]:
        api('PUT',resource if resource.startswith('/users/') else resource+'/activation',{'active':False})
        details=api('GET',prefix+'/deliveries/'+delivery,headers=headers)
        assert not details['delivery']['canReplay']
        api('POST',prefix+'/deliveries/'+delivery+'/replay',expected=409,headers={**headers,'Idempotency-Key':'inactive-'+resource.split('/')[-1]})
        api('PUT',resource if resource.startswith('/users/') else resource+'/activation',{'active':True})
    with lock: statuses['/api-check'] = 200
    def replay(_): return api('POST', prefix+'/deliveries/'+delivery+'/replay', expected=202, headers={**headers, 'Idempotency-Key': 'replay-once'})
    with ThreadPoolExecutor(max_workers=4) as executor: receipts = list(executor.map(replay, range(4)))
    assert all(item == receipts[0] for item in receipts)
    replay_id = receipts[0]['id']
    assert replay_id != delivery
    replayed = wait_for(lambda: api('GET', prefix+'/deliveries/'+replay_id, headers=headers), lambda data: data['delivery']['status'] == 'SUCCEEDED')
    assert len(replayed['attempts']) == 1 and replayed['requestBody'] == original['requestBody']
    assert replayed['delivery']['replayOfId'] == delivery
    assert api('GET', prefix+'/deliveries/'+delivery, headers=headers)['delivery']['status'] == 'DEAD'
    api('POST', prefix+'/deliveries/'+delivery+'/replay', expected=409, headers={**headers, 'Idempotency-Key': 'another-replay'})
    api('POST', prefix+'/deliveries/'+replay_id+'/replay', expected=409, headers={**headers, 'Idempotency-Key': 'successful-replay'})
    assert api('GET', prefix+'/events/'+receipt['id'], headers=headers)['status'] == 'SUCCEEDED'
    assert api('GET', prefix+'/deliveries?status=DEAD&includeReplayed=false', headers=headers)['totalElements'] == 0
    assert api('GET', prefix+'/deliveries?status=DEAD&includeReplayed=true', headers=headers)['totalElements'] == 1
    assert api('GET', prefix+'/activity-summary', headers=headers)['replays'] == 1
    api('POST', prefix+'/event-types', {'name': 'monitoring.empty'}, 201)
    for n in range(22): api('POST', prefix+'/events', {'eventType': 'monitoring.empty', 'payload': {'n': n}}, 202, {**headers, 'Idempotency-Key': f'empty-{n}'})
    first = api('GET', prefix+'/events?page=0&size=20', headers=headers)
    second = api('GET', prefix+'/events?page=1&size=20', headers=headers)
    assert len(first['items']) == 20 and len(second['items']) == 3 and first['totalElements'] == 23
    assert api('GET', prefix+'/events?status=NO_SUBSCRIBERS', headers=headers)['totalElements'] == 22
    assert api('GET', prefix+'/events?q=monitoring.empty', headers=headers)['totalElements'] == 22
    # Simulate a missed final event before the repair cursor passes this delivery.
    before=len(calls)
    sql(f"DELETE FROM service_a.status_inbox WHERE delivery_id='{replay_id}'; DELETE FROM service_a.delivery_attempt_projections WHERE delivery_id='{replay_id}'; UPDATE service_a.delivery_projections SET status='PENDING',last_sequence=0,attempt_count=0 WHERE id='{replay_id}'; UPDATE service_a.reconciliation_cursors SET finalized_at='1970-01-01T00:00:00Z',delivery_id='00000000-0000-0000-0000-000000000000'")
    recovered=wait_for(lambda:api('GET',prefix+'/deliveries/'+replay_id,headers=headers),lambda data:data['delivery']['status']=='SUCCEEDED' and len(data['attempts'])==1)
    assert recovered['attempts'][0]['httpStatus']==200 and len(calls)==before
    for path, timeout in [('/large', 2000), ('/stream', 200)]:
        name = 'monitoring.'+path[1:]
        kind = api('POST', prefix+'/event-types', {'name': name}, 201)['id']
        api('POST', prefix+'/endpoints', {'url': receiver_url+path, 'eventTypeIds': [kind], 'configuration': {'enabled': True, 'rateLimitPerSecond': 10, 'concurrencyLimit': 4, 'timeoutMs': timeout, 'maxAttempts': 1}}, 201)
        event = api('POST', prefix+'/events', {'eventType': name, 'payload': {}}, 202, {**headers, 'Idempotency-Key': path})
        final = wait_for(lambda: api('GET', prefix+'/events/'+event['id'], headers=headers), lambda data: data['status'] != 'PENDING')
        data = api('GET', prefix+'/deliveries/'+final['deliveries'][0]['id'], headers=headers)
        attempt = data['attempts'][0]
        if path == '/large': assert len(attempt['responseBody']) == 4096 and attempt['responseTruncated'] and data['delivery']['status'] == 'SUCCEEDED'
        else: assert data['delivery']['status'] == 'DEAD' and attempt['error'] == 'Response read timed out'
    print(f'Backend integration passed: {checks} HTTP checks, pagination, replay idempotency, history export and bounded responses.', flush=True)

try:
    if int(subprocess.check_output(['node', '-p', 'process.versions.node.split(".")[0]'], text=True).strip()) < 22:
        raise RuntimeError('Use Node 22.12+ for this test runner.')
    sql('CREATE DATABASE '+DATABASE, 'postgres'); database_created = True
    subprocess.run(['docker', 'run', '-d', '--name', BROKER, '-p', f'127.0.0.1:{broker_port}:5672', '-e', 'RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS=+S 2:2 +A 4', '-e', 'RABBITMQ_CTL_ERL_ARGS=+S 1:1 +A 2', 'rabbitmq:4-management'], check=True, stdout=subprocess.DEVNULL)
    for _ in range(120):
        try:
            with socket.create_connection(('127.0.0.1', broker_port), timeout=1): break
        except OSError:
            time.sleep(.5)
    else: raise RuntimeError('Test RabbitMQ did not become ready; see '+str(ARTIFACTS))
    # Packages must be built before running this harness; README provides commands.
    sql((ROOT/'service-a/src/main/resources/db/schema.sql').read_text())
    start('service-a', 'service-a', port_a); start('service_b', 'service-b', port_b)
    if not BROWSER_ONLY:
        spec=importlib.util.spec_from_file_location('company_checks',ROOT/'UI/e2e/company-checks.py')
        company=importlib.util.module_from_spec(spec);spec.loader.exec_module(company)
        company.verify_company(base,sql,receiver_url)
        verify_backend()
        metadata_spec=importlib.util.spec_from_file_location('metadata_checks',ROOT/'UI/e2e/metadata-checks.py')
        metadata_checks=importlib.util.module_from_spec(metadata_spec);metadata_spec.loader.exec_module(metadata_checks)
        metadata_checks.verify_metadata(api,receiver_url)
        activation_spec=importlib.util.spec_from_file_location('activation_checks',ROOT/'UI/e2e/activation-checks.py')
        activation_checks=importlib.util.module_from_spec(activation_spec);activation_spec.loader.exec_module(activation_checks)
        activation_checks.verify_activation(api,receiver_url)
        snapshot_spec=importlib.util.spec_from_file_location('snapshot_checks',ROOT/'UI/e2e/snapshot-checks.py')
        snapshots=importlib.util.module_from_spec(snapshot_spec);snapshot_spec.loader.exec_module(snapshots)
        snapshots.verify_snapshots(api,sql,wait_for,receiver_url,calls)
        ingress_spec=importlib.util.spec_from_file_location('ingress_checks',ROOT/'UI/e2e/ingress-checks.py')
        ingress_checks=importlib.util.module_from_spec(ingress_spec);ingress_spec.loader.exec_module(ingress_checks)
        ingress_checks.verify_ingress(base,sql,receiver_url)
        reconciliation_spec=importlib.util.spec_from_file_location('reconciliation_checks',ROOT/'UI/e2e/reconciliation-checks.py')
        reconciliation=importlib.util.module_from_spec(reconciliation_spec);reconciliation_spec.loader.exec_module(reconciliation)
        reconciliation.verify_reconciliation(api,sql,wait_for,receiver_url,port_b,ARTIFACTS/'internal.key',calls)
    env = dict(os.environ, API_PROXY_TARGET=f'http://127.0.0.1:{port_a}', E2E_API_URL=base,
               E2E_RECEIVER_URL=receiver_url, E2E_UI_PORT=str(ui_port), E2E_RUN_ID=RUN)
    result = subprocess.run(['npm', 'run', 'test:e2e', '--', *sys.argv[1:]], cwd=ROOT/'UI', env=env)
    if result.returncode: raise RuntimeError('Browser checks failed; inspect UI/test-results and '+str(ARTIFACTS))
    print(('PASS: focused browser checks. ' if BROWSER_ONLY else 'PASS: complete UI and backend integration. ')+'Isolated service logs: '+str(ARTIFACTS), flush=True)
finally:
    for process in reversed(processes):
        process.terminate()
        try: process.wait(timeout=15)
        except subprocess.TimeoutExpired: process.kill(); process.wait()
    for log in logfiles: log.close()
    receiver.shutdown(); receiver.server_close()
    with open(ARTIFACTS/'rabbitmq.log','w') as broker_log:
        subprocess.run(['docker','logs',BROKER],stdout=broker_log,stderr=subprocess.STDOUT)
    subprocess.run(['docker', 'rm', '-f', '-v', BROKER], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    if database_created: sql('DROP DATABASE '+DATABASE+' WITH (FORCE)', 'postgres')
    print('Disposed test database, broker and service processes; existing services/data were untouched.', flush=True)

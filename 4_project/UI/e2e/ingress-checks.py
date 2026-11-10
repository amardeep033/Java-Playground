"""Incoming submission audit is separate from deliveries and replay."""
import http.cookiejar
import json
import time
import urllib.error
import urllib.request
import uuid

def verify_ingress(base,sql,receiver):
    class Browser:
        def __init__(self):
            self.jar=http.cookiejar.CookieJar()
            self.open=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
        def call(self,method,path,body=None,expected=200,headers=None,raw=None,csrf=True):
            extra={'Content-Type':'application/json',**(headers or {})}
            if csrf and method not in ('GET','HEAD') and 'X-API-Key' not in extra:
                token,_=self.call('GET','/auth/csrf');extra[token['headerName']]=token['token']
            request=urllib.request.Request(base+path,method=method,headers=extra,data=raw if raw is not None else None if body is None else json.dumps(body).encode())
            try: response=self.open.open(request,timeout=10)
            except urllib.error.HTTPError as error:response=error
            with response:
                data=response.read()
                assert response.status==expected,(method,path,response.status,data)
                return (json.loads(data) if data else None),dict(response.headers)
        def register(self):
            email='ingress-'+uuid.uuid4().hex+'@example.com'
            user,_=self.call('POST','/users',{'companyName':'Ingress company','name':'Ingress checks','email':email,'password':'ExamplePass123!'},201)
            return user
    owner=Browser();user=owner.register()
    app,_=owner.call('POST',f'/users/{user["id"]}/applications',{'companyName':'Ingress company','name':'Ingress checks'},201)
    root=f'/applications/{app["id"]}'
    apppath=f'/users/{user["id"]}/applications/{app["id"]}'
    kind,_=owner.call('POST',root+'/event-types',{'name':'ingress.received'},201)
    typepath=root+'/event-types/'+kind['id']
    issued,_=owner.call('POST',root+'/endpoints',{'url':receiver+'/ingress-active','eventTypeIds':[kind['id']]},201)
    endpoint=issued['endpoint']
    other,_=owner.call('POST',root+'/endpoints',{'url':receiver+'/ingress-inactive','eventTypeIds':[kind['id']]},201)
    inactive=other['endpoint']['id']
    owner.call('PUT',root+'/endpoints/'+inactive+'/admin',{'active':False})
    issued_key,_=owner.call('POST',root+'/api-keys',{'name':'Order Producer Key 19'},201);key=issued_key['secret']
    body={'eventType':'ingress.received','payload':{'orderId':'audit-test'}}
    def row(headers):
        request_id=next(v for k,v in headers.items() if k.lower()=='x-request-id')
        deadline=time.monotonic()+5
        while True:
            data,_=owner.call('GET',root+'/incoming-requests?q='+request_id)
            if data['totalElements']:return data['items'][0]
            assert time.monotonic()<deadline,'Missing incoming audit record'
            time.sleep(.05)
    def submit(expected,reason,body=body,headers=None,raw=None):
        data,response_headers=owner.call('POST',root+'/events',body,expected,{'X-API-Key':key,**(headers or {})},raw)
        entry=row(response_headers)
        assert entry['reasonCode']==reason,entry
        assert entry['outcome']==('ACCEPTED' if expected==202 else 'REJECTED'),entry
        if expected!=202:assert entry['eventId'] is None
        return data,entry
    assert owner.call('GET',root+'/incoming-requests')[0]['totalElements']==0
    submit(400,'IDEMPOTENCY_KEY_MISSING')
    submit(400,'IDEMPOTENCY_KEY_INVALID',headers={'Idempotency-Key':'a'*201})
    submit(400,'MALFORMED_REQUEST',raw=b'{"eventType":"ingress.received","payload":"do-not-store-this-body"')
    submit(400,'INVALID_FIELDS',body={'eventType':'','payload':{}})
    submit(400,'PAYLOAD_REQUIRED',body={'eventType':'ingress.received','payload':None},headers={'Idempotency-Key':'null-payload'})
    submit(401,'UNAUTHORIZED',headers={'X-API-Key':'invalid','Idempotency-Key':'invalid-key'})
    # CSRF rejection happens before MVC, but is still captured by the outer audit filter.
    _,response=owner.call('POST',root+'/events',body,403,csrf=False)
    assert row(response)['reasonCode']=='CSRF_REJECTED'
    owner.call('PUT',apppath,{'active':False})
    submit(409,'APPLICATION_INACTIVE',headers={'Idempotency-Key':'paused-app'})
    owner.call('PUT',apppath,{'active':True})
    owner.call('PUT',typepath+'/activation',{'active':False})
    submit(409,'EVENT_TYPE_INACTIVE',headers={'Idempotency-Key':'paused-type'})
    owner.call('PUT',typepath+'/activation',{'active':True})
    submit(404,'EVENT_TYPE_NOT_FOUND',body={'eventType':'unknown.event','payload':{}},headers={'Idempotency-Key':'unknown-type'})
    # Force a transactional outbox failure; event/projection roll back but rejection audit commits.
    sql("ALTER TABLE service_a.outbox_messages ADD CONSTRAINT ingress_test_failure CHECK (payload->>'eventType'<>'ingress.received') NOT VALID")
    try:submit(409,'CONFLICT',headers={'Idempotency-Key':'transaction-rollback'})
    finally:sql('ALTER TABLE service_a.outbox_messages DROP CONSTRAINT ingress_test_failure')
    assert sql(f"SELECT count(*) FROM service_a.received_events WHERE application_id='{app['id']}'")=='0'
    assert sql(f"SELECT count(*) FROM service_a.delivery_projections WHERE application_id='{app['id']}'")=='0'
    assert sql(f"SELECT count(*) FROM service_b.deliveries WHERE application_id='{app['id']}'")=='0'
    assert sql(f"SELECT count(*) FROM service_a.outbox_messages WHERE payload->>'applicationId'='{app['id']}'")=='0'
    receipt,entry=submit(202,'INACTIVE_ENDPOINTS_SKIPPED',headers={'Idempotency-Key':'accepted'})
    assert entry['eventId']==receipt['id'] and entry['skippedEndpoints']==[inactive] and receipt['deliveryCount']==1
    assert entry['apiKeyName']=='Order Producer Key 19' and entry['apiKeyId']==issued_key['key']['id']
    assert sql(f"SELECT incoming_request_id FROM service_a.received_events WHERE id='{receipt['id']}'")==entry['id']
    assert owner.call('GET',root+'/events/'+receipt['id'])[0]['incomingRequestId']==entry['id']
    assert sql("SELECT count(*) FROM information_schema.columns WHERE table_schema='service_a' AND table_name='incoming_requests' AND column_name='event_id'")=='0'
    matched,_=owner.call('GET',root+'/incoming-requests?apiKeyName=producer&authMethod=API_KEY&httpStatus=400&status=REJECTED')
    assert matched['totalElements']>=3 and all(r['apiKeyName']=='Order Producer Key 19' and r['httpStatus']==400 for r in matched['items'])
    malformed=[r for r in matched['items'] if r['reasonCode']=='MALFORMED_REQUEST']
    assert malformed and malformed[0]['eventType'] is None and malformed[0]['apiKeyId']==issued_key['key']['id']
    assert owner.call('GET',root+'/incoming-requests?q=Order%20Producer')[0]['totalElements']>0
    owner.call('GET',root+'/incoming-requests?authMethod=INVALID',expected=400)
    owner.call('GET',root+'/incoming-requests?httpStatus=999',expected=400)
    duplicate,duplicate_entry=submit(202,'DUPLICATE_ACCEPTED',headers={'Idempotency-Key':'accepted'})
    assert duplicate==receipt and duplicate_entry['id']!=entry['id']
    assert duplicate_entry['duplicateOfId']==entry['id']
    submit(409,'IDEMPOTENCY_CONFLICT',body={'eventType':'ingress.received','payload':{'different':True}},headers={'Idempotency-Key':'accepted'})
    owner.call('PUT',root+'/endpoints/'+endpoint['id']+'/admin',{'active':False})
    no_targets,entry2=submit(202,'INACTIVE_ENDPOINTS_SKIPPED',headers={'Idempotency-Key':'all-inactive'})
    assert no_targets['deliveryCount']==0 and len(entry2['skippedEndpoints'])==2
    # Audit IDs are not delivery IDs and cannot be replayed or dead-lettered.
    owner.call('POST',root+'/deliveries/'+entry2['id']+'/replay',expected=404,headers={'Idempotency-Key':'cannot-replay-audit'})
    assert sql(f"SELECT count(*) FROM service_a.replay_requests WHERE application_id='{app['id']}'")=='0'
    for _ in range(21):submit(400,'IDEMPOTENCY_KEY_MISSING')
    first,_=owner.call('GET',root+'/incoming-requests?status=REJECTED&size=10&q=IDEMPOTENCY_KEY_MISSING')
    second,_=owner.call('GET',root+'/incoming-requests?status=REJECTED&size=10&page=1&q=IDEMPOTENCY_KEY_MISSING')
    assert first['totalElements']==22 and len(first['items'])==10 and len(second['items'])==10
    assert not ({r['id'] for r in first['items']}&{r['id'] for r in second['items']})
    data,_=owner.call('GET',root+'/incoming-requests?size=100')
    encoded=json.dumps(data)
    assert key not in encoded and 'do-not-store-this-body' not in encoded and 'ExamplePass123!' not in encoded
    owner.call('GET',root+'/incoming-requests?status=DEAD',expected=400)
    other=Browser();other.register();other.call('GET',root+'/incoming-requests',expected=403)
    other.call('GET','/auth/admin/incoming-requests',expected=404)
    anonymous=Browser();anonymous.call('GET',root+'/incoming-requests',expected=401,headers={'X-API-Key':key})
    # Preserve the historical name snapshot after a key is renamed/revoked.
    owner.call('PUT',root+'/api-keys/'+issued_key['key']['id'],{'name':'Renamed revoked producer'})
    owner.call('DELETE',root+'/api-keys/'+issued_key['key']['id'],expected=204)
    _,revoked=submit(401,'UNAUTHORIZED',headers={'Idempotency-Key':'revoked-key'})
    assert revoked['apiKeyName']=='Renamed revoked producer'
    assert owner.call('GET',root+'/incoming-requests?apiKeyName=Order%20Producer')[0]['totalElements']>0
    # An unassignable target is visible only in the ADMIN-wide audit.
    unknown=str(uuid.uuid4())
    _,unknown_headers=anonymous.call('POST',f'/applications/{unknown}/events',body,401,headers={'X-API-Key':key,'Idempotency-Key':'unknown-app'})
    print('Ingress integration passed: company-scoped incoming requests, privacy, rejection tracking and pagination.',flush=True)

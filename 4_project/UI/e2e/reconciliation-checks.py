"""Final-only messages, private REST feed and durable repair checkpoints."""
import json
import time
import uuid
import urllib.request
import urllib.error
import urllib.parse

def verify_reconciliation(api,sql,wait_for,receiver_url,port_b,key_file,calls):
    user=api('POST','/users',{'name':'Reconciliation owner','companyName':'Repair company','email':uuid.uuid4().hex+'@example.com','password':'ExamplePass123!'},201)
    app=api('POST',f'/users/{user["id"]}/applications',{'name':'Repair app'},201)
    root='/applications/'+app['id']
    kind=api('POST',root+'/event-types',{'name':'repair.test'},201)
    # Long enough to observe transient retry state; the UI projection remains pending.
    config={'enabled':True,'rateLimitPerSecond':100,'concurrencyLimit':2,'timeoutMs':1000,'maxAttempts':10}
    issued=api('POST',root+'/endpoints',{'name':'Retry destination','url':receiver_url+'/api-check','eventTypeIds':[kind['id']],'configuration':config},201)
    # The shared receiver's control API lets this test run independently of earlier replay checks.
    urllib.request.urlopen(urllib.request.Request(receiver_url+'/control',data=json.dumps({'path':'/api-check','status':503}).encode(),headers={'Content-Type':'application/json'})).close()
    event=api('POST',root+'/events',{'eventType':'repair.test','payload':{}},202,{'Idempotency-Key':str(uuid.uuid4())})
    delivery=api('GET',root+'/events/'+event['id'])['deliveries'][0]['id']
    wait_for(lambda:sql(f"SELECT status FROM service_b.deliveries WHERE id='{delivery}'"),lambda status:status=='RETRY_SCHEDULED')
    assert sql(f"SELECT count(*) FROM service_b.outbox_messages WHERE aggregate_id='{delivery}' AND exchange_name='event-relay.status'")=='0'
    pending=api('GET',root+'/deliveries/'+delivery)
    assert pending['delivery']['status']=='PENDING' and pending['attempts']==[]
    final=wait_for(lambda:api('GET',root+'/deliveries/'+delivery),lambda data:data['delivery']['status']=='DEAD')
    assert len(final['attempts'])==10
    assert sql(f"SELECT is_final FROM service_b.deliveries WHERE id='{delivery}'")=='t'
    assert sql(f"SELECT count(*) FROM service_b.outbox_messages WHERE aggregate_id='{delivery}' AND exchange_name='event-relay.status'")=='1'
    assert sql(f"SELECT payload->>'status' FROM service_b.outbox_messages WHERE aggregate_id='{delivery}' AND exchange_name='event-relay.status'")=='DEAD'
    token=key_file.read_text().strip()
    def feed(params=None,auth=True,expected=200):
        url=f'http://127.0.0.1:{port_b}/internal/v1/delivery-completions?'+urllib.parse.urlencode(params or {})
        req=urllib.request.Request(url,headers={'Authorization':'Bearer '+token} if auth else {})
        try:response=urllib.request.urlopen(req,timeout=10)
        except urllib.error.HTTPError as error:response=error
        with response:
            body=response.read();assert response.status==expected,(response.status,body)
            return json.loads(body) if response.status==200 else None
    feed(auth=False,expected=401);feed({'size':21},expected=400)
    cursor={'after':'1970-01-01T00:00:00Z','afterId':'00000000-0000-0000-0000-000000000000','size':1}
    seen=set();found=False
    for _ in range(200):
        batch=feed(cursor)['items']
        if not batch:break
        row=batch[0];assert row['deliveryId'] not in seen;seen.add(row['deliveryId'])
        assert row['schemaVersion']==2 and row['status'] in ('SUCCEEDED','DEAD') and row['nextAttemptAt'] is None
        assert len(row['attempts'])==row['attemptCount'] and 'command' not in row
        assert issued['signingSecret'] not in json.dumps(row)
        if row['deliveryId']==delivery:found=True
        cursor.update(after=row['updatedAt'],afterId=row['deliveryId'])
    assert found
    # Force one page to fail. Its checkpoint and imported mutations must roll back together.
    other=api('POST',root+'/endpoints',{'name':'Other destination','url':receiver_url+'/other','eventTypeIds':[kind['id']]},201)['endpoint']
    previous_id=str(uuid.UUID(int=uuid.UUID(delivery).int-1))
    sql(f"BEGIN; SELECT 1 FROM service_a.reconciliation_cursors FOR UPDATE; UPDATE service_a.delivery_projections SET endpoint_id='{other['id']}' WHERE id='{delivery}'; UPDATE service_a.reconciliation_cursors SET finalized_at=(SELECT finalized_at FROM service_b.deliveries WHERE id='{delivery}'),delivery_id='{previous_id}'; COMMIT;")
    time.sleep(2.5)
    assert sql("SELECT delivery_id FROM service_a.reconciliation_cursors")==previous_id
    before=sum(path=='/api-check' for path,*_ in calls)
    sql(f"BEGIN; DELETE FROM service_a.status_inbox WHERE delivery_id='{delivery}'; DELETE FROM service_a.delivery_attempt_projections WHERE delivery_id='{delivery}'; UPDATE service_a.delivery_projections SET endpoint_id='{issued['endpoint']['id']}',status='PENDING',last_sequence=0,attempt_count=0,http_status=NULL,last_error=NULL WHERE id='{delivery}'; COMMIT;")
    repaired=wait_for(lambda:api('GET',root+'/deliveries/'+delivery),lambda data:data['delivery']['status']=='DEAD' and len(data['attempts'])==10)
    assert repaired['delivery']['attemptCount']==10
    assert sum(path=='/api-check' for path,*_ in calls)==before
    assert sql("SELECT delivery_id FROM service_a.reconciliation_cursors")==delivery
    print('Reconciliation passed: final-only outbox, private indexed REST pagination, pending retries, complete attempt history, failure rollback, cursor resume and missed-event repair without a webhook resend.',flush=True)

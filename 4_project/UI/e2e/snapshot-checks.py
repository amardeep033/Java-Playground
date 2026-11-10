"""Accepted dispatches retain settings across in-place endpoint/app changes."""
import json
import uuid
import urllib.request


def verify_snapshots(api, sql, wait_for, receiver, calls):
    user = api('POST', '/users', {'name': 'Snapshot owner', 'email': 'snapshot-'+uuid.uuid4().hex+'@example.com', 'password': 'ExamplePass123!'}, 201)
    app = api('POST', f'/users/{user["id"]}/applications', {'name': 'Snapshots'}, 201)
    root = '/applications/'+app['id']
    app_path = f'/users/{user["id"]}/applications/{app["id"]}'
    old_type = api('POST', root+'/event-types', {'name': 'snapshot.old'}, 201)
    new_type = api('POST', root+'/event-types', {'name': 'snapshot.new'}, 201)
    with urllib.request.urlopen(urllib.request.Request(receiver+'/control', data=json.dumps({'path': '/snapshot-old', 'status': 503}).encode(), headers={'Content-Type': 'application/json'})):
        pass
    config = {'enabled': True, 'rateLimitPerSecond': 20, 'concurrencyLimit': 2, 'timeoutMs': 1500, 'maxAttempts': 2}
    issued = api('POST', root+'/endpoints', {'name': 'Snapshot endpoint', 'url': receiver+'/snapshot-old', 'eventTypeIds': [old_type['id']], 'configuration': config}, 201)
    endpoint = issued['endpoint']; path = root+'/endpoints/'+endpoint['id']
    receipt = api('POST', root+'/events', {'eventType': old_type['name'], 'payload': {'stable': True}}, 202, {'Idempotency-Key': 'snapshot-old'})
    delivery = json.loads(sql(f"SELECT payload::text FROM service_a.outbox_messages WHERE payload->>'eventId'='{receipt['id']}'"))
    original = dict(delivery)
    api('PUT', path+'/admin', {'url': receiver+'/snapshot-new'})
    api('PUT', path, {'configuration':{'maxAttempts':1,'timeoutMs':4000,'rateLimitPerSecond':50,'concurrencyLimit':5}})
    api('PUT', path+'/admin', {'eventTypeIds': [new_type['id']]})
    api('POST', path+'/signing-secret/rotate', expected=201)
    api('PUT', path+'/admin', {'active': False})
    api('PUT', app_path, {'active': False})
    finished = wait_for(lambda: api('GET', root+'/deliveries/'+delivery['deliveryId']), lambda data: data['delivery']['status']=='DEAD')
    assert len(finished['attempts']) == 2
    assert finished['delivery']['endpointUrl'] == receiver+'/snapshot-old'
    stored = json.loads(sql(f"SELECT payload::text FROM service_a.outbox_messages WHERE id='{original['messageId']}'"))
    assert stored == original
    sent = [call for call in calls if {key.lower():value for key,value in call[2].items()}.get('webhook-id')==delivery['deliveryId']]
    assert len(sent)==2 and all(call[0]=='/snapshot-old' for call in sent)
    assert all(json.loads(call[1])['data']=={'stable': True} for call in sent)
    api('PUT', app_path, {'active': True})
    api('PUT', path+'/admin', {'active': True})
    new_event = api('POST', root+'/events', {'eventType': new_type['name'], 'payload': {}}, 202, {'Idempotency-Key': 'snapshot-new'})
    fresh = json.loads(sql(f"SELECT payload::text FROM service_a.outbox_messages WHERE payload->>'eventId'='{new_event['id']}'"))
    assert fresh['url']==receiver+'/snapshot-new' and fresh['maxAttempts']==1 and fresh['timeoutMs']==4000
    assert fresh['signingSecretEncrypted']!=original['signingSecretEncrypted']
    wait_for(lambda: api('GET', root+'/events/'+new_event['id']), lambda data: data['status']=='SUCCEEDED')
    no_destination = api('POST', root+'/events', {'eventType': old_type['name'], 'payload': {}}, 202, {'Idempotency-Key': 'snapshot-unmapped'})
    assert no_destination['deliveryCount']==0
    audit = api('GET', '/clients/'+user['clientId']+'/audit-logs?scopes='+app['id']+'&size=100')['items']
    assert {'ENDPOINT_UPDATED','CONFIG_UPDATED','EVENT_TYPE_MAPPINGS_UPDATED'} <= {row['action'] for row in audit}
    encoded = json.dumps(audit)
    assert issued['signingSecret'] not in encoded and original['signingSecretEncrypted'] not in encoded
    print('Snapshot checks passed: in-place edits audited; accepted dispatch and retries retain destination/configuration/credential; later requests use new settings.', flush=True)

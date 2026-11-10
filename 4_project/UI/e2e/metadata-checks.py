"""Checks metadata updates and scoped list searches in the disposable database."""
import uuid

def verify_metadata(api, receiver):
    user=api('GET','/auth/me')['user']['id']
    prefix=f'/users/{user}/applications'
    apps=[api('POST',prefix,{'name':f'Metadata app {n:02d}'},201) for n in range(22)]
    app=apps[-1]['id']; root=f'/applications/{app}'
    updated=api('PUT',prefix+'/'+app,{'name':'Renamed metadata app','description':'Unique Needle 100%_safe'})
    assert updated['id']==app and updated['description']=='Unique Needle 100%_safe'
    for query in ['unique%20needle','RENAMED',app,'100%25_safe']:
        matches=api('GET',prefix+'?q='+query+'&page=0&size=1')
        assert matches['totalElements']==1 and matches['items'][0]['id']==app
    assert api('GET',prefix+'?q=absent-value')['totalElements']==0
    api('GET',prefix+'?q='+'a'*201,expected=400)
    api('PUT',prefix+'/'+app,{'name':apps[0]['name'],'description':None},expected=409)
    api('PUT',prefix+'/'+app,{'name':' ','description':None},expected=400)
    api('PUT',prefix+'/'+app,{'name':'No','userId':str(uuid.uuid4())},expected=400)
    types=[api('POST',root+'/event-types',{'name':f'metadata.event{n}','description':'Common definition'},201) for n in range(3)]
    kind=types[0]['id']
    old=api('GET',root+'/event-types/'+kind)
    api('PUT',root+'/event-types/'+kind,{'description':'Updated definition'})
    current=api('GET',root+'/event-types/'+kind)
    assert current['name']==old['name'] and current['createdAt']==old['createdAt']
    assert api('GET',root+'/event-types?q=updated')['items'][0]['id']==kind
    assert api('GET',root+'/event-types?q=metadata&page=1&size=2')['totalElements']==3
    assert api('GET',root+'/event-types?q='+kind)['totalElements']==1
    api('PUT',root+'/event-types/'+kind,{'description':'No','name':'breaking.rename'},expected=400)
    endpoint=api('POST',root+'/endpoints',{'url':receiver+'/metadata','description':'Original destination','eventTypeIds':[kind]},201)['endpoint']
    path=root+'/endpoints/'+endpoint['id']
    changed=api('PUT',path,{'name':'Orders webhook','description':'Updated destination'})
    assert changed['name']=='Orders webhook'
    assert api('GET',root+'/endpoints?q=Orders%20webhook')['totalElements']==1
    api('PUT',path,{'name':' '},expected=400)
    assert changed['url']==endpoint['url'] and changed['eventTypeIds']==endpoint['eventTypeIds']
    assert changed['signingSecretVersion']==endpoint['signingSecretVersion'] and changed['configuration']==endpoint['configuration']
    for body in [{'url':receiver+'/different'},{'eventTypeIds':[]},{'configuration':{'enabled':False}},{'signingSecret':'bad'}]:
        api('PUT',path,body,expected=400)
    assert api('GET',root+'/endpoints?q=updated&status=ENABLED')['totalElements']==1
    assert api('GET',root+'/endpoints?status=DISABLED')['totalElements']==0
    api('GET',root+'/endpoints?status=INVALID',expected=400)
    assert api('GET',root+'/endpoints?q='+endpoint['id'])['totalElements']==1
    key=api('POST',root+'/api-keys',{'name':'Metadata machine'},201)
    keypath=root+'/api-keys/'+key['key']['id']
    changed=api('PUT',keypath,{'name':'Renamed machine'})
    assert changed['prefix']==key['key']['prefix'] and changed['revokedAt'] is None
    assert api('GET',root+'/api-keys?q=renamed&status=ACTIVE')['totalElements']==1
    assert api('GET',root+'/api-keys?q='+key['key']['id'])['totalElements']==1
    api('PUT',keypath,{'name':'No','keyHash':'bad'},expected=400)
    api('PUT',keypath,{'name':'No','applicationId':str(uuid.uuid4())},expected=400)
    # The same API key still authenticates and routing still uses the original event name.
    receipt=api('POST',root+'/events',{'eventType':old['name'],'payload':{'test':'metadata'}},202,{'X-API-Key':key['secret'],'Idempotency-Key':'metadata-before'})
    detail=api('GET',root+'/events/'+receipt['id'])
    assert detail['eventType']==old['name']
    api('PUT',root+'/event-types/'+kind,{'description':None})
    assert api('GET',root+'/events/'+receipt['id'])['payload']==detail['payload']
    api('DELETE',keypath,expected=204)
    assert api('GET',root+'/api-keys?status=REVOKED')['totalElements']==1
    assert api('GET',root+'/api-keys?status=ACTIVE')['totalElements']==0
    api('GET',root+'/api-keys?status=INVALID',expected=400)
    api('PUT',f'/users/{user}',{'name':'Updated display name'})
    assert api('GET','/auth/me')['user']['name']=='Updated display name'
    api('PUT',f'/users/{user}',{'name':'No','email':'changed@example.com'},expected=400)
    api('DELETE',root+'/event-types/'+types[1]['id'],expected=204)
    assert api('GET',root+'/event-types?q='+types[1]['id'])['totalElements']==0
    api('PUT',root+'/event-types/'+types[1]['id'],{'description':'Deleted'},expected=404)
    api('POST','/users',{'name':'Other owner','email':'metadata-other-'+uuid.uuid4().hex+'@example.com','password':'ExamplePass123!'},201)
    for resource,body in [(prefix+'/'+app,{'name':'Foreign'}),(root+'/event-types/'+kind,{'description':'Foreign'}),(path,{'description':'Foreign'}),(keypath,{'name':'Foreign'})]:
        api('PUT',resource,body,expected=403)
    print('Metadata integration passed: scoped search, pagination, safe edits, immutable fields, retained audit and ownership.',flush=True)

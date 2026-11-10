"""Activation lifecycle checks against the runner's disposable database."""
import uuid
import time

def verify_activation(api, receiver):
    user=api('POST','/users',{'name':'Activation checks','email':'activation-'+uuid.uuid4().hex+'@example.com','password':'ExamplePass123!'},201)
    app=api('POST',f'/users/{user["id"]}/applications',{'name':'Activation'},201)
    apppath=f'/users/{user["id"]}/applications/{app["id"]}'
    root=f'/applications/{app["id"]}'
    kind=api('POST',root+'/event-types',{'name':'activation.check'},201)
    typepath=root+'/event-types/'+kind['id']
    endpoint=api('POST',root+'/endpoints',{'url':receiver+'/activation','eventTypeIds':[kind['id']]},201)['endpoint']
    endpath=root+'/endpoints/'+endpoint['id']
    def activation(path):return path if path==apppath else path+('/admin' if path==endpath else '/activation')
    key=api('POST',root+'/api-keys',{'name':'Activation key'},201)['secret']
    headers={'X-API-Key':key}
    def send(key,expected=202):
        return api('POST',root+'/events',{'eventType':'activation.check','payload':{}},expected,{**headers,'Idempotency-Key':key})
    for path in [apppath,typepath,endpath]:
        assert api('GET',path)['active']
        api('PUT',activation(path),{},400)
        api('PUT',activation(path),{'active':False,'unexpected':True},400)
    # Only applications combine metadata and activation.
    combined=api('PUT',apppath,{'name':'Combined app','description':'Preserve this description','active':False})
    assert combined['active'] is False and combined['description']=='Preserve this description'
    api('PUT',apppath,{'active':True})
    assert api('GET',apppath)['description']=='Preserve this description'
    api('PUT',apppath,{'description':None})
    assert api('GET',apppath)['description'] is None
    api('PUT',apppath,{'active':None},400)
    api('PUT',apppath+'/activation',{'active':False},404)
    for path in [typepath,endpath]:
        api('PUT',path,{'description':'Preserve child metadata'})
        api('PUT',path,{'description':'Denied','active':False},400)
        before=api('GET',path)
        api('PUT',activation(path),{'active':False})
        after=api('GET',path)
        assert after['description']=='Preserve child metadata' and after['name']==before['name']
        api('PUT',activation(path),{'active':True})
    api('PUT',typepath,{'active':False,'name':'breaking.rename'},400)
    assert api('GET',typepath)['active'] is True
    changes=api('GET','/clients/'+user['clientId']+'/audit-logs?size=100&scopes='+app['id'])['items']
    assert any(row['details'].get('field')=='name' for row in changes)
    assert any(row['details'].get('field')=='is_active' for row in changes)
    assert not any(row['details'].get('field')=='description' for row in changes)
    original=send('accepted')
    # Already accepted work finishes even when every related configuration is paused.
    for path in [apppath,typepath,endpath]:api('PUT',activation(path),{'active':False})
    deadline=time.monotonic()+20
    while True:
        state=api('GET',root+'/events/'+original['id'],headers=headers)['status']
        if state=='SUCCEEDED':break
        assert time.monotonic()<deadline, state
        time.sleep(.2)
    for path in [apppath,typepath,endpath]:api('PUT',activation(path),{'active':True})
    for path,listpath in [(apppath,f'/users/{user["id"]}/applications'),(typepath,root+'/event-types')]:
        assert api('PUT',activation(path),{'active':False})['active'] is False
        assert api('GET',listpath+'?status=INACTIVE')['totalElements']==1
        assert api('GET',listpath+'?status=ACTIVE')['totalElements']==0
        send('paused-'+path.split('/')[-1],409)
        assert send('accepted')['id']==original['id']
        assert api('GET',root+'/events/'+original['id'],headers=headers)['id']==original['id']
        assert api('PUT',activation(path),{'active':True})['active']
    # Endpoint pauses preserve subscriptions and produce no new delivery for that target.
    api('PUT',activation(endpath),{'active':False})
    assert api('GET',endpath)['configuration']['enabled'] is False
    assert api('GET',root+'/endpoints?status=INACTIVE')['totalElements']==1
    assert send('no-target')['deliveryCount']==0
    api('PUT',activation(endpath),{'active':True})
    assert send('resumed')['deliveryCount']==1
    # App activation does not overwrite a child's independent inactive flag.
    api('PUT',activation(typepath),{'active':False})
    api('PUT',apppath,{'active':False})
    api('PUT',apppath,{'active':True})
    assert api('GET',typepath)['active'] is False
    api('PUT',activation(typepath),{'active':True})
    # Different owners cannot pause resources, and deletion cannot be undone by activation.
    api('POST','/users',{'name':'Other','email':'other-active-'+uuid.uuid4().hex+'@example.com','password':'ExamplePass123!'},201)
    for path in [apppath,typepath,endpath]:api('PUT',activation(path),{'active':False},403)
    api('POST','/auth/login',{'email':user['email'],'password':'ExamplePass123!'})
    api('DELETE',typepath,expected=204)
    api('PUT',activation(typepath),{'active':True},404)
    assert api('GET',root+'/events',headers=headers)['totalElements']==3
    api('DELETE',endpath,expected=204)
    api('PUT',activation(endpath),{'active':True},404)
    api('DELETE',apppath,expected=204)
    api('PUT',apppath,{'active':True},403)
    print('Activation integration passed: pause/resume, filters, new-work gating, idempotent receipts, retained history, ownership and irreversible soft delete.',flush=True)

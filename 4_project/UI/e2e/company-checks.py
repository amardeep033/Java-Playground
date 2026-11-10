"""Company isolation, two-role permissions, audits and in-place endpoint contracts."""
import http.cookiejar
import json
import urllib.request
import urllib.error
import uuid
import copy
import hashlib
from datetime import datetime,timezone
from concurrent.futures import ThreadPoolExecutor

def verify_company(base,sql,receiver):
    checks=0
    class Browser:
        def __init__(self):
            self.jar=http.cookiejar.CookieJar();self.open=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
            self.email='company-'+uuid.uuid4().hex+'@example.com';self.password='ExamplePass123!'
        def call(self,method,path,body=None,expected=200,headers=None,csrf=True):
            nonlocal checks
            extra={'Content-Type':'application/json',**(headers or {})}
            if csrf and method not in ('GET','HEAD') and 'X-API-Key' not in extra:
                token=self.call('GET','/auth/csrf');extra[token['headerName']]=token['token']
            request=urllib.request.Request(base+path,method=method,data=None if body is None else json.dumps(body).encode(),headers=extra)
            try: response=self.open.open(request,timeout=15)
            except urllib.error.HTTPError as error:response=error
            with response:
                raw=response.read();assert response.status==expected,(method,path,response.status,raw)
                checks+=1;return json.loads(raw) if raw else None
        def register(self):
            self.user=self.call('POST','/users',{'name':'Company owner','companyName':'Company '+uuid.uuid4().hex,'email':self.email,'password':self.password},201);return self.user['id']
        def login(self,password=None,expected=200):return self.call('POST','/auth/login',{'email':self.email,'password':password or self.password},expected)
        def cookie(self):return next(c for c in self.jar if c.name=='ER_ACCESS')
    owner,foreign,normal,second=Browser(),Browser(),Browser(),Browser()
    user=owner.register();foreign.register();client=owner.user['clientId'];cp='/clients/'+client
    assert owner.user['role']=='ADMIN' and owner.call('GET','/auth/me')['permissions']=={'manageUsers':True,'createApplications':True}
    assert sql("SELECT count(*) FROM information_schema.tables WHERE table_schema='service_a' AND table_name='client_members'")=='0'
    assert sql("SELECT count(*) FROM service_a.users WHERE client_id IS NULL")=='0'
    app=owner.call('POST',f'/users/{user}/applications',{'name':'Orders'},201);root='/applications/'+app['id']
    sibling=owner.call('POST',f'/users/{user}/applications',{'name':'Payments'},201)
    normal.user=owner.call('POST',cp+'/users',{'name':'Bob','email':normal.email,'password':normal.password,'role':'USER','applicationIds':[app['id']]},201);normal.login()
    second.user=owner.call('POST',cp+'/users',{'name':'Alice colleague','email':second.email,'password':second.password,'role':'ADMIN','applicationIds':[]},201);second.login()
    assert normal.call('GET',f'/users/{normal.user["id"]}/applications')['totalElements']==1
    assert second.call('GET',f'/users/{second.user["id"]}/applications')['totalElements']==2
    listed=owner.call('GET',cp+'/users?size=1&q=Bob')
    assert len(listed['items'])==1 and listed['items'][0]['applicationIds']==[app['id']]
    assert {item['id'] for item in listed['applications']}=={app['id'],sibling['id']}
    assert all(item['active'] for item in listed['applications'])
    empty=owner.call('GET',cp+'/users?q=missing-user-search')
    assert empty['items']==[] and len(empty['applications'])==2
    foreign.call('GET',cp,expected=403);foreign.call('GET',cp+'/users',expected=403)
    normal.call('GET',cp+'/users',expected=403)
    for actor in (normal,foreign):
        actor.call('POST',cp+'/users',{'name':'Denied','email':'denied@example.com','password':'ExamplePass123!','role':'ADMIN','applicationIds':[]},403)
        actor.call('PUT',cp+'/users/'+second.user['id'],{'role':'USER'},403)
    for suffix in ('events','deliveries','incoming-requests','api-keys','event-types','endpoints'):
        foreign.call('GET',root+'/'+suffix,expected=403)
    owner.call('PUT',cp+'/users/'+foreign.user['id'],{'role':'ADMIN'},404)
    owner.call('PUT','/users/'+foreign.user['id'],{'name':'Denied'},403)
    owner.call('POST',cp+'/users',{'name':'Bad role','email':'bad@example.com','password':'ExamplePass123!','role':'APP_ADMIN'},400)
    foreign_app=foreign.call('POST',f'/users/{foreign.user["id"]}/applications',{'name':'Foreign'},201)
    owner.call('POST',cp+'/users',{'name':'Bad mapping','email':'bad-map@example.com','password':'ExamplePass123!','role':'USER','applicationIds':[foreign_app['id']]},400)
    normal.call('PUT',f'/users/{normal.user["id"]}/applications/{app["id"]}',{'name':'Denied'},403)
    normal.call('POST',f'/users/{normal.user["id"]}/applications',{'name':'Denied'},403)
    kind=owner.call('POST',root+'/event-types',{'name':'order.created'},201)
    normal.call('PUT',root+'/event-types/'+kind['id'],{'description':'Allowed'})
    normal.call('PUT',root+'/event-types/'+kind['id'],{'name':'breaking.rename'},400)
    issued=owner.call('POST',root+'/endpoints',{'name':'Customer','url':receiver+'/company','eventTypeIds':[kind['id']]},201);endpoint=issued['endpoint'];ep=root+'/endpoints/'+endpoint['id']
    normal.call('PUT',ep,{'name':'Named by user'})
    for path,data in ((ep+'/admin',{'url':receiver+'/denied'}),(ep+'/admin',{'active':False}),(root+'/event-types/'+kind['id']+'/activation',{'active':False}),(ep+'/admin',{'eventTypeIds':[kind['id']]})):
        normal.call('PUT',path,data,403)
    # Even unchanged active is admin-only; mixed unauthorized updates cannot partially save metadata.
    before=normal.call('GET',ep)
    normal.call('PUT',ep,{'name':'Denied mixed update','description':'Denied','active':False},400)
    normal.call('PUT',ep,{'description':'Denied','active':True},400)
    after=normal.call('GET',ep)
    assert after['name']==before['name'] and after['description']==before['description'] and after['active']==before['active']
    normal.call('PUT',root+'/event-types/'+kind['id'],{'description':'Denied','active':False},400)
    assert normal.call('GET',root+'/event-types/'+kind['id'])['description']=='Allowed'
    config=normal.call('PUT',ep,{'configuration':{key:value for key,value in {**endpoint['configuration'],'maxAttempts':7}.items() if key!='enabled'}});assert config['configuration']['maxAttempts']==7
    assert normal.call('PUT',ep,{'configuration':{key:value for key,value in config['configuration'].items() if key!='enabled'}})['configuration']['maxAttempts']==7
    new=second.call('PUT',ep+'/admin',{'url':receiver+'/company-new'});assert new['url']==receiver+'/company-new'
    # Two update routes: common fields preserve omitted values and reject admin fields atomically.
    normal.call('PUT',ep,{'description':'Preserved endpoint description'})
    limits={'rateLimitPerSecond':31,'concurrencyLimit':3,'timeoutMs':2500,'maxAttempts':7}
    combined=normal.call('PUT',ep,{'name':'Common atomic edit','configuration':limits})
    assert combined['name']=='Common atomic edit' and combined['description']=='Preserved endpoint description'
    normal.call('PUT',ep,{'name':'Denied common edit','configuration':{**limits,'enabled':False}},400)
    assert normal.call('GET',ep)['name']=='Common atomic edit'
    normal.call('PUT',ep+'/admin',{'url':receiver+'/denied','active':False,'eventTypeIds':[kind['id']]},403)
    admin_edit=owner.call('PUT',ep+'/admin',{'url':receiver+'/combined','active':False,'eventTypeIds':[kind['id']]})
    assert not admin_edit['active'] and admin_edit['url']==receiver+'/combined'
    owner.call('PUT',ep+'/admin',{'active':True})
    foreign_kind=foreign.call('POST','/applications/'+foreign_app['id']+'/event-types',{'name':'foreign.contract'},201)
    owner.call('PUT',ep+'/admin',{'url':receiver+'/rollback','active':False,'eventTypeIds':[foreign_kind['id']]},400)
    unchanged=owner.call('GET',ep);assert unchanged['active'] and unchanged['url']==receiver+'/combined'
    for suffix in ('configuration','activation','url','subscriptions'):
        owner.call('PUT',ep+'/'+suffix,{'active':False},404)
    def change(n):
        actor=Browser();actor.email=second.email;actor.password=second.password;actor.login()
        return actor.call('PUT',ep+'/admin',{'url':receiver+'/company-'+str(n)})
    with ThreadPoolExecutor(max_workers=3) as pool:results=list(pool.map(change,range(3)))
    assert len(results)==3 and all('configurationVersion' not in row for row in results)
    assert sql("SELECT to_regclass('service_a.endpoint_versions') IS NULL")=='t'
    owner.call('GET',ep+'/versions',expected=404)
    key=owner.call('POST',root+'/api-keys',{'name':'Machine'},201)
    normal.call('PUT',root+'/api-keys/'+key['key']['id'],{'name':'Renamed machine'})
    # Promotions are client-wide and checked live, even with an old USER JWT.
    owner.call('PUT',cp+'/users/'+normal.user['id'],{'role':'ADMIN'},204)
    assert normal.call('GET',f'/users/{normal.user["id"]}/applications')['totalElements']==2
    owner.call('PUT',cp+'/users/'+normal.user['id'],{'role':'USER'},204)
    assert normal.call('GET',f'/users/{normal.user["id"]}/applications')['totalElements']==0
    owner.call('GET',root+'/members',expected=405)
    owner.call('PUT',root+'/members',{'userId':normal.user['id']},204)
    owner.call('PUT',root+'/members',{'userId':foreign.user['id']},400)
    normal.call('GET','/applications/'+sibling['id']+'/events',expected=403)
    owner.call('PUT','/users/'+normal.user['id'],{'name':'Updated by admin'})
    normal.call('PUT','/users/'+normal.user['id'],{'name':'Updated by self'})
    email='renamed-'+uuid.uuid4().hex+'@example.com';owner.call('PUT',cp+'/users/'+normal.user['id'],{'email':email},204);normal.email=email
    owner.call('PUT',cp+'/users/'+normal.user['id']+'/password',{'newPassword':'ResetPassword123!'},204);normal.password='ResetPassword123!';normal.login()
    logs=json.dumps(owner.call('GET',cp+'/audit-logs?size=100'))
    assert 'ROLE_CHANGED' in logs and 'PASSWORD_CHANGED' in logs
    for secret in ('ExamplePass123!','ResetPassword123!',key['secret'],issued['signingSecret']):assert secret not in logs
    # Audit filtering is enforced before pagination and cannot escape the company.
    owner.call('PUT',f'/users/{user}/applications/{app["id"]}',{'name':'Orders renamed'})
    owner.call('PUT',f'/users/{user}/applications/{sibling["id"]}',{'name':'Payments renamed'})
    context=owner.call('GET',cp+'/audit-logs?scopes=overall,'+app['id']+'&size=100')
    assert any(row['application_id'] is None for row in context['items'])
    assert any(row['application_id']==app['id'] for row in context['items'])
    assert all(row['client_id']==client and row['application_id'] in (None,app['id']) for row in context['items'])
    only_app=owner.call('GET',cp+'/audit-logs?scopes='+app['id']+'&size=100')
    assert only_app['items'] and all(row['application_id']==app['id'] for row in only_app['items'])
    company=owner.call('GET',cp+'/audit-logs?scopes=overall&resources=user&actions=update&size=100')
    assert company['items'] and all(row['application_id'] is None and row['resource']=='user' and row['display_action']=='update' for row in company['items'])
    assert owner.call('GET',cp+'/audit-logs?size=1')['totalElements']>1
    assert owner.call('GET',cp+'/audit-logs?q=NoMatchingAuditValue')['totalElements']==0
    owner.call('GET',cp+'/audit-logs?resources=invalid',expected=400)
    owner.call('GET',cp+'/audit-logs?sort=invalid',expected=400)
    assert owner.call('GET',cp+'/audit-logs?scopes='+foreign_app['id'])['totalElements']==0
    foreign.call('GET',cp+'/audit-logs',expected=403)
    normal.call('GET',cp+'/audit-logs',expected=403)
    owner.call('GET',root+'/audit-logs',expected=404)
    actors=owner.call('GET',cp+'/audit-logs?actors='+user+'&actions=create,update&resources=user,app&sort=asc')
    assert actors['items'] and all(row['actor_id']==user and row['resource'] in ('user','app') and row['display_action'] in ('create','update') for row in actors['items'])
    assert [row['created_at'] for row in actors['items']]==sorted(row['created_at'] for row in actors['items'])
    owner.call('PUT',cp+'/users/'+normal.user['id'],{'name':'Single editor name','email':normal.email,'role':'USER'},204)
    assert owner.call('GET','/users/'+normal.user['id'])['name']=='Single editor name'
    owner.call('POST','/auth/logout',expected=403,csrf=False)
    anonymous=Browser();anonymous.call('POST','/users',{'name':'No company','email':'missing@example.com','password':'ExamplePass123!'},400)
    owner.call('GET',root+'/events',headers={'X-API-Key':'invalid'},expected=401)
    # Preserve at least one ADMIN, including simultaneous demotions.
    competitors=[]
    for target in (user,second.user['id']):
        actor=Browser();actor.email=owner.email;actor.password=owner.password;actor.login();competitors.append((actor,target))
    def demote(pair):
        actor,target=pair
        try:actor.call('PUT',cp+'/users/'+target,{'role':'USER'},204);return 204
        except AssertionError as error:
            status=error.args[0][2];assert status in (403,409);return status
    with ThreadPoolExecutor(max_workers=2) as pool:statuses=list(pool.map(demote,competitors))
    assert statuses.count(204)==1
    assert sql(f"SELECT count(*) FROM service_a.users WHERE client_id='{client}' AND role='ADMIN' AND deleted_at IS NULL")=='1'
    last=owner if sql(f"SELECT role FROM service_a.users WHERE id='{user}'")=='ADMIN' else second
    last.call('PUT',cp+'/users/'+last.user['id'],{'role':'USER'},409)
    last.call('DELETE',cp+'/users/'+last.user['id'],expected=409)
    foreign.call('GET','/auth/admin/users',expected=404)
    # Opaque refresh credentials: rotation, replay detection, expiry and revocation.
    refresh_browser=Browser(); refresh_user=refresh_browser.register()
    def refresh_cookie(browser): return next(c for c in browser.jar if c.name=='ER_REFRESH')
    def copied_refresh(browser):
        result=Browser(); result.jar.set_cookie(copy.copy(refresh_cookie(browser))); return result
    refresh_cookie_before=copy.copy(refresh_cookie(refresh_browser))
    raw=refresh_cookie_before.value
    digest=hashlib.sha256(raw.encode()).hexdigest()
    assert len(raw)==43 and '.' not in raw
    assert refresh_cookie_before.has_nonstandard_attr('HttpOnly') and refresh_cookie_before.path=='/api/v1/auth'
    assert refresh_cookie_before.get_nonstandard_attr('SameSite')=='Lax'
    assert 29*86400 < refresh_cookie_before.expires-datetime.now(timezone.utc).timestamp() <= 30*86400
    family=sql(f"SELECT family_id FROM service_a.refresh_tokens WHERE token_hash='{digest}'")
    expiry=sql(f"SELECT expires_at FROM service_a.refresh_token_families WHERE id='{family}'")
    assert sql(f"SELECT count(*) FROM service_a.refresh_tokens WHERE token_hash='{raw}'")=='0'
    refresh_browser.call('POST','/auth/refresh',expected=403,csrf=False)
    refresh_browser.cookie().value='expired-or-invalid-access'
    renewed=refresh_browser.call('POST','/auth/refresh')
    assert renewed['user']['id']==refresh_user and 'refreshToken' not in renewed
    assert refresh_cookie(refresh_browser).value!=raw
    assert sql(f"SELECT used_at IS NOT NULL FROM service_a.refresh_tokens WHERE token_hash='{digest}'")=='t'
    assert sql(f"SELECT expires_at FROM service_a.refresh_token_families WHERE id='{family}'")==expiry
    refresh_browser.call('GET','/auth/me')
    stale=Browser(); stale.jar.set_cookie(refresh_cookie_before)
    stale.call('POST','/auth/refresh',expected=401)
    assert sql(f"SELECT revocation_reason FROM service_a.refresh_token_families WHERE id='{family}'")=='REUSE_DETECTED'
    refresh_browser.call('POST','/auth/refresh',expected=401)

    refresh_browser.login()
    stolen=copied_refresh(refresh_browser)
    refresh_browser.cookie().value='expired-access'
    refresh_browser.call('POST','/auth/logout',expected=204)
    assert not any(c.name in ('ER_ACCESS','ER_REFRESH') for c in refresh_browser.jar)
    stolen.call('POST','/auth/refresh',expected=401)
    refresh_browser.login()
    another=Browser(); another.email=refresh_browser.email; another.password=refresh_browser.password; another.login()
    refresh_browser.call('PUT',f'/users/{refresh_user}/password',{'currentPassword':refresh_browser.password,'newPassword':'ChangedPass123!'},expected=204)
    another.call('GET','/auth/me')  # Access JWT verification remains stateless.
    another.call('POST','/auth/refresh',expected=401)
    assert sql(f"SELECT count(*) FROM service_a.refresh_token_families WHERE user_id='{refresh_user}' AND revoked_at IS NULL")=='0'
    refresh_browser.password='ChangedPass123!'; refresh_browser.login()
    family=sql(f"SELECT family_id FROM service_a.refresh_tokens WHERE token_hash='{hashlib.sha256(refresh_cookie(refresh_browser).value.encode()).hexdigest()}'")
    sql(f"UPDATE service_a.refresh_token_families SET created_at=now()-interval '31 days',expires_at=now()-interval '1 day' WHERE id='{family}'")
    refresh_browser.call('POST','/auth/refresh',expected=401)
    refresh_browser.login()
    family=sql(f"SELECT family_id FROM service_a.refresh_tokens WHERE token_hash='{hashlib.sha256(refresh_cookie(refresh_browser).value.encode()).hexdigest()}'")
    sql(f"UPDATE service_a.refresh_token_families SET expires_at=now()+interval '30 seconds' WHERE id='{family}'")
    capped=refresh_browser.call('POST','/auth/refresh')
    cap=float(sql(f"SELECT extract(epoch FROM expires_at) FROM service_a.refresh_token_families WHERE id='{family}'"))
    assert datetime.fromisoformat(capped['expiresAt'].replace('Z','+00:00')).timestamp()<=cap
    # Exactly one concurrent rotation succeeds; the other revokes the family as reuse.
    refresh_browser.login()
    simultaneous=[copied_refresh(refresh_browser),copied_refresh(refresh_browser)]
    def rotate_once(browser):
        try: browser.call('POST','/auth/refresh'); return 200
        except AssertionError as failure:
            assert failure.args[0][2]==401; return 401
    with ThreadPoolExecutor(max_workers=2) as executor:
        assert sorted(executor.map(rotate_once,simultaneous))==[200,401]
    refresh_browser.call('POST','/auth/refresh',expected=401)
    print(f'Company contracts passed: {checks} requests; two roles, company isolation, account administration, mapping, audit privacy, in-place endpoint updates and refresh rotation.',flush=True)

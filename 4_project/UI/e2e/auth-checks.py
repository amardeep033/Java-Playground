"""Basic auth contract checks; only used against the runner's disposable database."""
from concurrent.futures import ThreadPoolExecutor
import http.cookiejar
import json
import urllib.request
import urllib.error
import uuid


def verify_auth(base, sql):
    checks = 0
    class Browser:
        def __init__(self):
            self.jar = http.cookiejar.CookieJar()
            self.open = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
        def call(self, method, path, body=None, expected=200, csrf=True, headers=None):
            nonlocal checks
            extra = dict(headers or {})
            if csrf and method not in ('GET', 'HEAD'):
                extra['X-XSRF-TOKEN'] = self.call('GET', '/auth/csrf')['token']
            req = urllib.request.Request(base+path, method=method, data=None if body is None else json.dumps(body).encode(), headers={'Content-Type':'application/json', **extra})
            try: response = self.open.open(req, timeout=10)
            except urllib.error.HTTPError as error: response = error
            with response:
                raw = response.read()
                assert response.status == expected, (method, path, response.status, raw)
                checks += 1
                return json.loads(raw) if raw else None
        def register(self):
            self.email = 'auth-'+uuid.uuid4().hex+'@example.com'
            self.password = 'ExamplePass123!'
            self.user = self.call('POST', '/users', {'name':'Auth checks','email':self.email,'password':self.password,'role':'ADMIN'},201)
            return self.user['id']
        def login(self, password=None, expected=200):
            return self.call('POST','/auth/login',{'email':self.email,'password':password or self.password},expected)
        def cookie(self):
            return next(cookie for cookie in self.jar if cookie.name == 'ER_SESSION')

    anonymous = Browser()
    anonymous.call('GET','/auth/me',expected=401)
    anonymous.call('POST','/auth/login',{'email':'unknown@example.com','password':'wrong'},expected=403,csrf=False)
    owner, stranger = Browser(), Browser()
    user, other_user = owner.register(), stranger.register()
    assert owner.call('GET','/auth/me')['user']['id'] == user
    assert owner.call('GET','/auth/me')['role'] == 'USER'
    cookie = owner.cookie()
    assert cookie.has_nonstandard_attr('HttpOnly') and cookie.path == '/api/v1'
    assert cookie.get_nonstandard_attr('SameSite') == 'Lax'
    app = owner.call('POST',f'/users/{user}/applications',{'name':'Private'},201)['id']
    root = f'/applications/{app}'
    owner.call('POST',root+'/event-types',{'name':'auth.check'},201)
    owner.call('POST',root+'/events',{'eventType':'auth.check','payload':{}},202,headers={'Idempotency-Key':'dashboard'})
    owner.call('GET',root+'/events')
    owner.call('GET',root+'/deliveries')
    owner.call('GET',root+'/activity-summary')
    stranger.call('GET',f'/users/{user}',expected=403)
    stranger.call('POST',f'/users/{user}/applications',{'name':'No'},expected=403)
    stranger.call('GET',root+'/event-types',expected=403)
    stranger.call('POST',root+'/events',{'eventType':'auth.check','payload':{}},expected=403,headers={'Idempotency-Key':'foreign'})
    owner.call('POST',root+'/event-types',{'name':'csrf.check'},expected=403,csrf=False)
    owner.call('GET','/auth/admin/users',expected=403)
    sql(f"UPDATE service_a.users SET role='ADMIN' WHERE id='{other_user}'")
    assert stranger.call('GET','/auth/me')['role'] == 'ADMIN'
    stranger.call('GET','/auth/admin/users')
    stranger.call('GET',root+'/event-types')
    stranger.call('GET',root+'/deliveries')
    stranger.call('PUT',f'/users/{user}/password',{'currentPassword':owner.password,'newPassword':'MaliciousPass123!'},expected=403)
    sql(f"UPDATE service_a.users SET role='USER' WHERE id='{other_user}'")
    stranger.call('GET','/auth/admin/users',expected=403)
    key = owner.call('POST',root+'/api-keys',{'name':'Machine key'},201)['secret']
    anonymous.call('GET',root+'/events',headers={'X-API-Key':key})
    anonymous.call('GET',root+'/api-keys',headers={'X-API-Key':key},expected=401)
    anonymous.call('POST',root+'/events',{'eventType':'auth.check','payload':{}},expected=401,csrf=False,headers={'X-API-Key':'er_'+'a'*43,'Idempotency-Key':'forged'})
    # An API-key header must not let an ambient cookie bypass CSRF or API-key validation.
    owner.call('POST',root+'/events',{'eventType':'auth.check','payload':{}},expected=401,csrf=False,headers={'X-API-Key':'invalid','Idempotency-Key':'no-fallback'})
    original = cookie.value
    cookie.value = original[:-8] + 'AAAAAAAA'
    owner.call('GET','/auth/me',expected=401)
    cookie.value = original
    owner.call('GET','/auth/me')
    owner.login('wrong-password',401)
    owner.login('wrong-password',401)
    history = owner.call('GET','/auth/login-attempts')
    assert len([row for row in history if not row['success']]) == 2
    assert all('password' not in row for row in history)
    owner.login()  # Success resets the failure counter.
    for _ in range(5): owner.login('wrong-password',401)
    owner.login(expected=429)
    sql(f"UPDATE service_a.login_guards SET locked_until=now()-interval '1 second'")
    owner.login()
    # A second browser, logout-all and password changes revoke server-side JWT sessions.
    second = Browser(); second.email=owner.email; second.password=owner.password; second.login()
    owner.call('POST','/auth/logout-all',expected=204)
    second.call('GET','/auth/me',expected=401)
    owner.login(); second.login()
    owner.call('PUT',f'/users/{user}/password',{'currentPassword':owner.password,'newPassword':'ChangedPass123!'},expected=204)
    second.call('GET','/auth/me',expected=401)
    owner.login(expected=401)
    owner.password='ChangedPass123!'; owner.login()
    current = owner.cookie().value
    owner.call('POST','/auth/logout',expected=204)
    anonymous.call('GET','/auth/me',expected=401,headers={'Cookie':'ER_SESSION='+current})
    owner.login()
    sql(f"UPDATE service_a.auth_sessions SET expires_at=now()-interval '1 second' WHERE user_id='{user}'")
    owner.call('GET','/auth/me',expected=401)
    owner.login()
    owner.call('DELETE',f'/users/{user}',expected=204)
    owner.call('GET','/auth/me',expected=401)
    # Deleted and unknown accounts receive the same generic credential error.
    unknown = Browser(); unknown.email='unknown-'+uuid.uuid4().hex+'@example.com'; unknown.password='ExamplePass123!'
    assert owner.login(expected=401)['detail'] == unknown.login(expected=401)['detail']
    for _ in range(4): unknown.login('wrong-password',401)
    unknown.login(expected=429)
    parallel_email='parallel-'+uuid.uuid4().hex+'@example.com'
    def fail_once(_):
        isolated=Browser(); isolated.email=parallel_email; isolated.password='wrong-password'
        isolated.login(expected=401)
    with ThreadPoolExecutor(max_workers=5) as executor: list(executor.map(fail_once, range(5)))
    isolated=Browser(); isolated.email=parallel_email; isolated.password='wrong-password'
    isolated.login(expected=429)
    print(f'Auth integration passed: {checks} requests; CSRF, ownership/RBAC, cookies, lockout, audit, logout and password revocation.',flush=True)

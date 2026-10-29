"""Live account lifecycle smoke test. Uses and deletes its own QA account only."""
import requests
import secrets
import uuid

def main():
    base='https://prototyx.onrender.com'
    client=requests.Session()
    credentials={'email':'qa-account-'+uuid.uuid4().hex+'@example.invalid','password':secrets.token_urlsafe(24)}
    def call(method,path,body=None,expected=200):
        r=client.request(method,base+path,json=body,timeout=60)
        assert r.status_code==expected, f'{path}: HTTP {r.status_code}'
        print(path+': PASS',flush=True)
        return r.json() if r.headers.get('content-type','').startswith('application/json') else r.text
    def login():
        result=call('POST','/api/auth/login',credentials)
        client.headers['Authorization']='Bearer '+result['accessToken']
    data=call('POST','/api/auth/register',credentials)
    client.headers['Authorization']='Bearer '+data['accessToken']
    deleted=False
    try:
        call('GET','/api/account')
        call('POST','/api/account/profile',{'name':'Isolated QA account'})
        call('POST','/api/holdings/sync',{'holdings':{'AAPL':1}})
        exported=call('GET','/api/account/export')
        assert exported['holdings']=={'AAPL':1} and 'password' not in str(exported)
        ready=call('GET','/api/auth/recovery-status')
        print('Recovery email configured: '+str(ready['available']),flush=True)
        for path in ['/legal/privacy','/legal/terms','/account/reset','/account/delete']: call('GET',path)
        call('POST','/api/auth/logout')
        call('GET','/api/account',expected=401)
        login()
        call('POST','/api/account/signout-all',{'password':credentials['password']})
        call('GET','/api/account',expected=401)
        login()
        new_password=secrets.token_urlsafe(24)
        call('POST','/api/account/password',{'current_password':credentials['password'],'new_password':new_password})
        credentials['password']=new_password
        call('GET','/api/account',expected=401)
        login()
        call('POST','/api/account/delete',{'password':'wrong-password'},expected=400)
        call('POST','/api/account/delete',{'password':credentials['password']})
        deleted=True
        call('GET','/api/account',expected=401)
    finally:
        if not deleted:
            login();call('POST','/api/account/delete',{'password':credentials['password']})

if __name__=='__main__': main()

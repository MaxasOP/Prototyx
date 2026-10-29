"""Explicit live smoke test using a separate disposable QA account. No secrets printed."""
import json
import secrets
import uuid
import requests

def main():
    base = 'https://prototyx.onrender.com'
    client = requests.Session()
    credentials = {'email':'qa-'+uuid.uuid4().hex+'@example.invalid','password':secrets.token_urlsafe(24)}
    registered = client.post(base+'/api/auth/register', json=credentials, timeout=30)
    registered.raise_for_status()
    token = registered.json()['accessToken']
    client.headers['Authorization'] = 'Bearer '+token
    def check(method, path, payload=None, verify=None):
        try:
            response = client.request(method,base+path,json=payload,timeout=100)
            data = response.json()
            ok = response.ok and (verify(data) if verify else True)
            print(json.dumps({'path':path,'status':response.status_code,'passed':bool(ok),
                              'detail':data.get('detail') if isinstance(data,dict) and not response.ok else None}),flush=True)
            return data
        except Exception as error:
            print(json.dumps({'path':path,'passed':False,'error':type(error).__name__}),flush=True)
    check('GET','/api/status')
    check('POST','/api/auth/login',credentials,lambda d:bool(d.get('accessToken')))
    check('POST','/api/holdings/sync',{'holdings':{'AAPL':.4,'MSFT':.6}},lambda d:d=={'AAPL':.4,'MSFT':.6})
    check('POST','/api/holdings/sync',{'holdings':{'MSFT':1}},lambda d:d=={'MSFT':1})
    check('GET','/api/holdings',verify=lambda d:d=={'MSFT':1})
    status = check('GET','/api/status')
    print(json.dumps({'configured':status}),flush=True)
    check('GET','/api/finny/strategy-report',verify=lambda d:d.get('portfolioHealthScore')==0)
    check('GET','/api/market/chart/AAPL?period=1W',verify=lambda d:len(d.get('pricePoints',[]))>0)
    check('GET','/api/market/indicators/AAPL',verify=lambda d:d.get('close',0)>0)
    check('GET','/api/screener/search?query=AAPL',verify=lambda d:bool(d) and d[0]['ticker']=='AAPL')
    check('POST','/api/quant/optimize',{'tickers':['AAPL','MSFT']},lambda d:abs(sum(d['weights'].values())-1)<1e-5)
    check('POST','/api/quant/risk-mesh',{'tickers':['AAPL','MSFT'],'weights':[.5,.5]},lambda d:len(d.get('betas',{}))==2)
    check('POST','/api/quant/stress-test',{'tickers':['AAPL','MSFT'],'weights':[.5,.5],'scenarioId':'tech_selloff'},lambda d:'Historical replay' in d.get('scenarioName',''))
    check('GET','/api/earnings/transcript/AAPL?year=2026&quarter=3',verify=lambda d:len(d.get('prepared_remarks',''))>100)
    check('POST','/api/agents/debate',{'tickers':['AAPL']},lambda d:bool(d.get('debate_logs')))
    check('POST','/api/agents/consult',{'query':'Summarize the observed technical metrics for AAPL. Do not forecast.'},lambda d:bool(d.get('recommendation')))
    check('GET','/api/news/sentiment?ticker=AAPL',verify=lambda d:bool(d))
    check('POST','/api/holdings/sync',{'holdings':{}},lambda d:d=={})

if __name__=='__main__': main()

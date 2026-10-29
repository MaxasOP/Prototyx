"""Grounded research perspectives using Groq."""
import json
import os
from groq import Groq
from data.market_data import get_latest_metrics

def run_committee_debate(tickers):
    if not tickers or len(tickers)>10:
        raise ValueError('Select 1 to 10 tickers')
    key = os.getenv('GROQ_API_KEY')
    if not key: raise RuntimeError('GROQ_API_KEY is not configured')
    facts = [get_latest_metrics(t) for t in tickers]
    result = Groq(api_key=key, timeout=45).chat.completions.create(
        model=os.getenv('GROQ_MODEL','qwen/qwen3.8-27b'),
        response_format={'type':'json_object'},
        messages=[{'role':'system','content':'Discuss only supplied dated observations. Never invent macro conditions, earnings, price targets, return projections or confidence scores. Return JSON with debate_logs, a list of objects with agent and message. Use technical, fundamental and risk perspectives. Acknowledge missing evidence.'},
                  {'role':'user','content':json.dumps(facts)}])
    logs = json.loads(result.choices[0].message.content).get('debate_logs')
    if not isinstance(logs,list) or not logs or any(not isinstance(x,dict) or not isinstance(x.get('agent'),str) or not isinstance(x.get('message'),str) for x in logs):
        raise RuntimeError('Research provider returned an invalid response')
    return {'mode':'Groq research perspectives on supplied observations','debate_logs':logs,'implied_views':{},'confidences':[]}

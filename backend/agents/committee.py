"""Grounded research perspectives using Gemini or Groq."""
import json
import os
from agents.llm import generate_text
from data.market_data import get_latest_metrics

def run_committee_debate(tickers):
    if not tickers or len(tickers)>10:
        raise ValueError('Select 1 to 10 tickers')
    facts = [get_latest_metrics(t) for t in tickers]

    prompt = (
        'Discuss only supplied dated observations. Never invent macro conditions, earnings, '
        'price targets, return projections or confidence scores. Return JSON with debate_logs, '
        'a list of objects with agent and message. Use technical, fundamental and risk perspectives. '
        'Acknowledge missing evidence. Observations: ' + json.dumps(facts)
    )
    content = generate_text(prompt, json_mode=True)

    logs = json.loads(content).get('debate_logs')
    if not isinstance(logs,list) or not logs or any(not isinstance(x,dict) or not isinstance(x.get('agent'),str) or not isinstance(x.get('message'),str) for x in logs):
        raise RuntimeError('Research provider returned an invalid response')
    return {'mode':'AI research perspectives on supplied observations','debate_logs':logs,'implied_views':{},'confidences':[]}

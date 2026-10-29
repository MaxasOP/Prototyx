from typing import List, Dict, Any, Optional
import random

def search_screener_items(query: Optional[str] = None, risk_level: Optional[str] = None) -> List[Dict[str, Any]]:
    from data.market_data import get_latest_metrics, resolve_ticker
    import yfinance as yf
    items = []
    for company in search_tickers_catalog(query):
        metrics = get_latest_metrics(company['ticker'])
        symbol = yf.Ticker(resolve_ticker(company['ticker']))
        history = symbol.history(period='3mo', auto_adjust=True)
        volatility = float(history['Close'].pct_change().std() * (252 ** .5))
        level = 'SAFE' if volatility < .20 else 'BALANCED' if volatility < .35 else 'GROWTH'
        if risk_level and risk_level.upper() != level:
            continue
        change = float((history['Close'].iloc[-1]/history['Close'].iloc[-2]-1)*100) if len(history)>1 else 0
        summary = f'Historical annualized volatility {volatility:.1%}. Risk label is a volatility band, not a safety guarantee.'
        items.append(dict(company, price=metrics['close'], changePercent=change,
                          peRatio=metrics.get('pe_ratio'), dividendYield=metrics.get('dividend_yield'),
                          rsi=metrics.get('rsi'), riskLevel=level, currency=metrics.get('currency'),
                          aiAnalysis=summary, plainEnglishSummary=summary))
    return items

def search_tickers_catalog(query: Optional[str] = None, sector: Optional[str] = None) -> List[Dict[str, Any]]:
    catalog = [
        {"ticker": "TCS", "name": "Tata Consultancy Services", "sector": "Technology", "industry": "IT Services", "exchange": "NSE"},
        {"ticker": "RELIANCE", "name": "Reliance Industries Ltd", "sector": "Energy", "industry": "Oil & Gas Conglomerate", "exchange": "NSE"},
        {"ticker": "NVDA", "name": "NVIDIA Corporation", "sector": "Technology", "industry": "Semiconductors", "exchange": "NASDAQ"},
        {"ticker": "AAPL", "name": "Apple Inc", "sector": "Technology", "industry": "Consumer Electronics", "exchange": "NASDAQ"},
        {"ticker": "MSFT", "name": "Microsoft Corporation", "sector": "Technology", "industry": "Software & Cloud", "exchange": "NASDAQ"},
        {"ticker": "INFY", "name": "Infosys Ltd", "sector": "Technology", "industry": "IT Services", "exchange": "NSE"}
    ]

    if query:
        q = query.lower()
        catalog = [c for c in catalog if q in c["ticker"].lower() or q in c["name"].lower()]
    if sector:
        catalog = [c for c in catalog if sector.lower() in c['sector'].lower()]
    return catalog

def calculate_macro_stress_test(tickers: List[str], weights: List[float], scenario_id: str, base_currency=None) -> Dict[str, Any]:
    import numpy as np
    from quant.history import returns_for
    if len(tickers)!=len(weights) or not weights or any(not np.isfinite(w) or w<0 for w in weights) or sum(weights)<=0:
        raise ValueError('Provide a nonnegative finite weight per ticker')
    # Replay actual historical market stress windows; no fabricated macro calibration.
    windows = {'rate_hike':('2022-01-01','2022-12-31','2022 tightening cycle'),
               'oil_spike':('2022-02-01','2022-06-30','2022 energy shock'),
               'tech_selloff':('2022-01-01','2022-10-31','2022 technology sell-off')}
    if scenario_id not in windows:
        raise ValueError('Unknown scenario')
    start,end,label = windows[scenario_id]
    history = returns_for(tickers, period='5y',base_currency=base_currency)
    w = np.array(weights)/sum(weights)
    portfolio = history @ w
    stressed = portfolio.loc[start:end]
    if len(stressed)<30:
        raise ValueError('Insufficient observations in the historical stress window')
    def drawdown(series):
        wealth = (1+series).cumprod()
        return float((wealth/wealth.cummax().clip(lower=1)-1).min())
    return {'scenarioName':'Historical replay: '+label,
            'portfolioReturnChange':float((1+stressed).prod()-1),
            'initialVaR':max(0,float(-portfolio.quantile(.05))),
            'stressedVaR':max(0,float(-stressed.quantile(.05))),
            'initialDrawdown':drawdown(portfolio), 'stressedDrawdown':drawdown(stressed),
            'aiHedgingStrategy':'Compare concentration and cash needs. No hedge is guaranteed by this historical replay.',
            'plainEnglishVerdict':'Replays historical daily returns at current constant weights. VaR is a daily historical 95% loss quantile; this is not a forecast.',
            'source':'Yahoo Finance adjusted daily returns'}


def get_finny_strategy_report(holdings):
    total = sum(holdings.values())
    if total<=0:
        raise ValueError('Add holdings before requesting a briefing')
    largest = max(holdings.values())/total
    score = round((1-largest)*100)
    summary = f'{len(holdings)} holdings. Largest normalized allocation: {largest:.1%}. This is a concentration summary, not a market-risk assessment.'
    return {'portfolioHealthScore':score, 'riskStatus':'CONCENTRATION ONLY', 'keyInsights':[summary],
            'suggestedRebalance':{}, 'selfLearnedMarketRegime':'Not assessed', 'dailyBriefingSummary':summary}

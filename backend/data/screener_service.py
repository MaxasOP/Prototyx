from typing import List, Dict, Any, Optional

def search_screener_items(query: Optional[str] = None, risk_level: Optional[str] = None) -> List[Dict[str, Any]]:
    from data.market_data import get_latest_metrics, resolve_ticker
    from data.market_data import price_history
    from concurrent.futures import ThreadPoolExecutor
    import math

    def load(company):
        try:
            metrics = get_latest_metrics(company['ticker'])
            history = price_history(resolve_ticker(company['ticker']), period='3mo')
            volatility = float(history['Close'].pct_change().std() * (252 ** .5))
            if not math.isfinite(volatility):
                return None
            level = 'SAFE' if volatility < .20 else 'BALANCED' if volatility < .35 else 'GROWTH'
            if risk_level and risk_level.upper() != level:
                return None
            change = float((history['Close'].iloc[-1]/history['Close'].iloc[-2]-1)*100) if len(history)>1 else 0
            summary = f'Historical annualized volatility {volatility:.1%}. Risk label is a volatility band, not a safety guarantee.'
            return dict(company, price=metrics['close'], changePercent=change,
                        peRatio=metrics.get('pe_ratio'), dividendYield=metrics.get('dividend_yield'),
                        rsi=metrics.get('rsi'), riskLevel=level, currency=metrics.get('currency'),
                        aiAnalysis=summary, plainEnglishSummary=summary)
        except RuntimeError:
            return None

    companies = search_tickers_catalog(query)
    with ThreadPoolExecutor(max_workers=4) as pool:
        items = [item for item in pool.map(load, companies) if item is not None]
    if companies and not items and not risk_level:
        raise RuntimeError('Market data unavailable for matching companies. Try again shortly.')
    return items


def search_tickers_catalog(query: Optional[str] = None, sector: Optional[str] = None) -> List[Dict[str, Any]]:
    from data.company_search import search_companies
    return search_companies(query, sector)

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

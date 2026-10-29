"""Validated historical long-only minimum-variance allocation."""
import numpy as np
from scipy.optimize import minimize
from quant.history import returns_for

def optimize_portfolio_mvo(tickers, views=None):
    r = returns_for(tickers)
    names = list(r.columns)
    mean = r.mean().to_numpy()*252
    covariance = r.cov().to_numpy()*252
    if views:
        if any(t not in names or not np.isfinite(v) or abs(v)>1 for t,v in views.items()):
            raise ValueError('Views must be finite decimal annual returns between -1 and 1')
        mean = np.array([views.get(t, mean[i]) for i,t in enumerate(names)])
    fit = minimize(lambda w: float(w@covariance@w), np.ones(len(names))/len(names),
                   bounds=[(0,1)]*len(names), constraints=[{'type':'eq','fun':lambda w:w.sum()-1}],
                   method='SLSQP', options={'ftol':1e-12})
    if not fit.success: raise ValueError('No feasible allocation found')
    w = fit.x
    vol = float(np.sqrt(w@covariance@w))
    ret = float(w@mean)
    return {'weights':dict(zip(names,w.tolist())), 'expected_annual_return':ret,
            'annual_volatility':vol, 'sharpe_ratio':ret/vol if vol else 0,
            'method':'Historical minimum variance (long only)',
            'ai_rationale':'Adjusted daily prices; annualized using 252 sessions. Sharpe assumes zero risk-free rate. Views change return estimates, not minimum-variance weights. Estimates are not forecasts.',
            'source':'Yahoo Finance historical prices', 'asOf':str(r.index[-1]), 'observations':len(r)}

def optimize_portfolio_black_litterman(tickers, views):
    raise ValueError('Black-Litterman is not implemented')

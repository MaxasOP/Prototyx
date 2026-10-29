from typing import Dict, List, Any
import numpy as np

def calculate_exposure_mesh(tickers: List[str], weights: List[float], benchmark=None, base_currency=None) -> Dict[str, Any]:
    """
    Computes real cross-asset risk mesh based on static sector correlations.
    No external APIs used.
    """
    from quant.history import returns_for
    from data.market_data import resolve_ticker
    if len(tickers) != len(weights) or not weights or not all(np.isfinite(w) and w >= 0 for w in weights) or sum(weights) <= 0:
        raise ValueError('Provide a finite nonnegative weight per ticker with positive total')
    benchmark = benchmark or ('^NSEI' if resolve_ticker(tickers[0]).endswith(('.NS', '.BO')) else '^GSPC')
    if benchmark not in ('^NSEI','^GSPC'): raise ValueError('Unsupported benchmark')
    universe = list(dict.fromkeys(tickers + [benchmark]))
    history = returns_for(universe,base_currency=base_currency)
    assets = history[tickers]
    corr = assets.corr()
    variance = history[benchmark].var()
    if variance <= 0 or not np.isfinite(corr.to_numpy()).all():
        raise ValueError('Insufficient price variation for risk estimates')
    betas = {t: float(history[t].cov(history[benchmark])/variance) for t in tickers}
    w = np.array(weights)/sum(weights)
    redundant = [{'ticker_1':a, 'ticker_2':b, 'correlation':float(corr.loc[a,b]), 'warning':'Historical correlation above 0.70'}
                 for i,a in enumerate(tickers) for b in tickers[i+1:] if corr.loc[a,b] > .7]
    return {'tickers':tickers, 'weights':w.tolist(), 'correlation_matrix':corr.to_dict(),
            'redundant_exposures':redundant, 'hedged_positions':[],
            'net_exposure_index':float(w@corr.to_numpy()@w), 'betas':betas,
            'portfolio_beta':float(w@np.array([betas[t] for t in tickers])),
            'ai_risk_audit':f'Historical daily-return estimates against {benchmark}; not a forecast or a guarantee of diversification.',
            'benchmark':benchmark, 'observations':len(history), 'asOf':str(history.index[-1])}

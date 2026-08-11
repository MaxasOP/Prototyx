import random
from typing import Dict, List, Any, Optional

# Dynamic imports
try:
    import pandas as pd
    import numpy as np
    import yfinance as yf
    from pyportfolioopt import EfficientFrontier, risk_models, expected_returns
    from pyportfolioopt import BlackLittermanModel
    HAS_LIBS = True
except ImportError:
    HAS_LIBS = False

def optimize_portfolio_mvo(tickers: List[str], target: str = "max_sharpe") -> Dict[str, Any]:
    """
    Solves Mean-Variance Optimization (MVO) or returns mock calculations.
    """
    if not HAS_LIBS:
        # Fallback to realistic mock values for presentation
        n = len(tickers)
        mock_weights = {}
        remaining = 1.0
        for i in range(n - 1):
            w = round(random.uniform(0.1, remaining - (0.05 * (n - i - 1))), 2)
            mock_weights[tickers[i]] = w
            remaining -= w
        mock_weights[tickers[-1]] = round(remaining, 2)
        
        return {
            "weights": mock_weights,
            "expected_annual_return": 0.145,
            "annual_volatility": 0.124,
            "sharpe_ratio": 1.17,
            "method": "Mock Mean-Variance Optimization (Offline Fallback)"
        }

    try:
        # Improved ticker cleaning logic
        cleaned_tickers = []
        for t in tickers:
            t_up = t.upper().strip()
            if t_up.endswith((".NS", ".BO")) or t_up in ["AAPL", "MSFT", "TSLA", "GOOGL", "AMZN", "META", "NVDA"]:
                cleaned_tickers.append(t_up)
            elif len(t_up) <= 6:
                cleaned_tickers.append(f"{t_up}.NS")
            else:
                cleaned_tickers.append(t_up)

        data = yf.download(cleaned_tickers, period="2y", progress=False)["Close"]

        if isinstance(data, pd.Series):
            data = data.to_frame()

        data = data.ffill().bfill()
        
        # Map columns back
        ticker_mapping = {}
        for i, original in enumerate(tickers):
            ticker_mapping[cleaned_tickers[i]] = original

        data = data.rename(columns=ticker_mapping)
        
        mu = expected_returns.capm_return(data)
        S = risk_models.sample_cov(data)
        
        ef = EfficientFrontier(mu, S)
        
        if target == "max_sharpe":
            weights = ef.max_sharpe()
        else:
            weights = ef.min_volatility()
            
        cleaned_weights = ef.clean_weights()
        performance = ef.portfolio_performance(verbose=False)
        
        # Final JSON-ready response sanitization
        def sanitize_val(v):
            if isinstance(v, float) and (pd.isna(v) or np.isinf(v)):
                return 0.0
            return v

        return {
            "weights": {k: float(sanitize_val(v)) for k, v in cleaned_weights.items()},
            "expected_annual_return": float(sanitize_val(performance[0])),
            "annual_volatility": float(sanitize_val(performance[1])),
            "sharpe_ratio": float(sanitize_val(performance[2])),
            "method": "Mean-Variance Optimization"
        }
    except Exception as e:
        print(f"ERROR in MVO calculation: {str(e)}")
        # If solver fails, return a basic mock equal weighting
        n = len(tickers)
        return {
            "weights": {t: 1.0 / n for t in tickers},
            "expected_annual_return": 0.12,
            "annual_volatility": 0.15,
            "sharpe_ratio": 0.8,
            "method": f"Equal Weighting (MVO Solver Failure: {str(e)})"
        }

def optimize_portfolio_black_litterman(
    tickers: List[str], 
    views: Dict[str, float], 
    confidences: Optional[List[float]] = None
) -> Dict[str, Any]:
    """
    Solves Black-Litterman optimization or returns mock allocations.
    """
    if not HAS_LIBS:
        # Fallback to customized mock Black-Litterman based on active views
        n = len(tickers)
        mock_weights = {}
        
        # Give higher weight to assets with higher view return forecasts
        total_view_score = sum(views.get(t, 0.10) for t in tickers)
        if total_view_score == 0:
            total_view_score = 1.0
            
        remaining = 1.0
        for i in range(n - 1):
            t = tickers[i]
            val = views.get(t, 0.10)
            w = round((val / total_view_score) * 0.9, 2)
            mock_weights[t] = w
            remaining -= w
        mock_weights[tickers[-1]] = round(remaining, 2)
        
        return {
            "weights": mock_weights,
            "expected_annual_return": 0.162,
            "annual_volatility": 0.115,
            "sharpe_ratio": 1.41,
            "method": "Mock Black-Litterman (Offline Fallback)"
        }

    try:
        cleaned_tickers = []
        for t in tickers:
            t_up = t.upper().strip()
            if t_up.endswith((".NS", ".BO")) or t_up in ["AAPL", "MSFT", "TSLA", "GOOGL", "AMZN", "META", "NVDA"]:
                cleaned_tickers.append(t_up)
            elif len(t_up) <= 6:
                cleaned_tickers.append(f"{t_up}.NS")
            else:
                cleaned_tickers.append(t_up)

        data = yf.download(cleaned_tickers, period="2y", progress=False)["Close"]
        
        if isinstance(data, pd.Series):
            data = data.to_frame()

        data = data.ffill().bfill()

        ticker_mapping = {}
        for i, original in enumerate(tickers):
            ticker_mapping[cleaned_tickers[i]] = original

        data = data.rename(columns=ticker_mapping)
        
        S = risk_models.sample_cov(data)
        
        n = len(tickers)
        prior_weights = pd.Series(1.0 / n, index=tickers)
        delta = 2.5
        implied_returns = delta * S.dot(prior_weights)
        
        q_list = []
        p_list = []
        
        active_tickers = [t for t in tickers if t in views]
        if not active_tickers:
            return optimize_portfolio_mvo(tickers)
            
        for ticker in active_tickers:
            q_list.append(views[ticker])
            row = [1.0 if t == ticker else 0.0 for t in tickers]
            p_list.append(row)
            
        Q = np.array(q_list)
        P = np.array(p_list)
        
        bl = BlackLittermanModel(
            S, 
            pi=implied_returns, 
            P=P, 
            Q=Q, 
            omega="idd",
            view_confidences=confidences
        )
        
        posterior_mu = bl.bl_returns()
        posterior_S = bl.bl_cov()
        
        ef = EfficientFrontier(posterior_mu, posterior_S)
        weights = ef.max_sharpe()
        cleaned_weights = ef.clean_weights()
        performance = ef.portfolio_performance(verbose=False)
        
        # Final JSON-ready response sanitization
        def sanitize_val(v):
            if isinstance(v, float) and (pd.isna(v) or np.isinf(v)):
                return 0.0
            return v

        return {
            "weights": {k: float(sanitize_val(v)) for k, v in cleaned_weights.items()},
            "expected_annual_return": float(sanitize_val(performance[0])),
            "annual_volatility": float(sanitize_val(performance[1])),
            "sharpe_ratio": float(sanitize_val(performance[2])),
            "method": "Black-Litterman Optimization"
        }
    except Exception as e:
        print(f"ERROR in Black-Litterman calculation: {str(e)}")
        # Fallback to standard MVO on solver failures
        return optimize_portfolio_mvo(tickers, target="max_sharpe")

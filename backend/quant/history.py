import numpy as np
import pandas as pd
import yfinance as yf
from data.market_data import resolve_ticker

def returns_for(tickers, period='2y', base_currency=None):
    if not tickers or len(tickers)>30 or len(set(tickers))!=len(tickers):
        raise ValueError('Select 1 to 30 distinct tickers')
    prices = {}
    currencies = set()
    currency_by_ticker = {}
    for ticker in tickers:
        asset = yf.Ticker(resolve_ticker(ticker))
        currency = asset.fast_info.get('currency')
        if not currency: raise ValueError('Currency unavailable for '+ticker)
        currencies.add(currency)
        currency_by_ticker[ticker] = currency
        history = asset.history(period=period, auto_adjust=True)
        if history.empty: raise ValueError('History unavailable for '+ticker)
        series = history['Close'].copy()
        series.index = pd.to_datetime(series.index.date)
        prices[ticker] = series
    target = base_currency or ('USD' if len(currencies)>1 else next(iter(currencies)))
    if target not in ('USD','INR'): raise ValueError('Supported analysis currencies are USD and INR')
    if currencies != {target}:
        for currency in currencies - {target}:
            fx = yf.Ticker(currency+target+'=X').history(period=period, auto_adjust=True)
            if fx.empty:
                raise ValueError('FX conversion unavailable for '+currency)
            rates = fx['Close'].copy()
            rates.index = pd.to_datetime(rates.index.date)
            for ticker in tickers:
                if currency_by_ticker[ticker] == currency:
                    # Align on observed dates only; do not fill future or missing FX observations.
                    prices[ticker] = prices[ticker] * rates.reindex(prices[ticker].index)
    result = pd.DataFrame(prices).dropna().pct_change(fill_method=None).dropna()
    if len(result)<60 or not np.isfinite(result.to_numpy()).all():
        raise ValueError('At least 60 aligned daily returns are required')
    return result

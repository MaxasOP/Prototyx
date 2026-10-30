from typing import Dict, Any, List
import yfinance as yf
import math
import numpy as np
import pandas as pd


def finite_number(value):
    try:
        number = float(value)
        return number if math.isfinite(number) else None
    except (TypeError, ValueError):
        return None


def price_history(symbol, period='1mo', interval='1d', asset=None):
    """Only observed, positive closes are suitable for charts and calculations."""
    history = (asset if asset is not None else yf.Ticker(symbol)).history(period=period, interval=interval, auto_adjust=True)
    if history.empty or 'Close' not in history:
        raise RuntimeError('Historical prices unavailable for ' + symbol)
    history = history.copy()
    history['Close'] = pd.to_numeric(history['Close'], errors='coerce')
    history = history.loc[np.isfinite(history['Close']) & (history['Close'] > 0)].sort_index()
    history = history.loc[~history.index.duplicated(keep='last')]
    if history.empty:
        raise RuntimeError('No valid closing prices available for ' + symbol)
    return history

INDIAN_BLUECHIPS = ["TCS", "RELIANCE", "INFY", "HDFCBANK", "ITC", "HINDUNILVR"]

TICKER_ALIASES = {
    "APPL": "AAPL",
    "GOOG": "GOOGL",
    "MSFT": "MSFT",
    "TSLA": "TSLA",
    "NVDA": "NVDA",
    "META": "META",
    "BTC": "BTC-USD",
    "ETH": "ETH-USD"
}

def resolve_ticker(ticker: str) -> str:
    t = ticker.upper().strip()
    if t in TICKER_ALIASES:
        t = TICKER_ALIASES[t]
    if t.endswith(('.NS', '.BO')):
        return t
    if t in INDIAN_BLUECHIPS:
        return f"{t}.NS"
    return t

def get_latest_metrics(ticker: str) -> Dict[str, Any]:
    """
    Computes indicators from observed Yahoo Finance prices.
    """
    ticker_clean = ticker.upper().strip()
    symbol = resolve_ticker(ticker_clean)

    try:
        data = yf.Ticker(symbol)
        hist = price_history(symbol, period="2y", asset=data)
        try:
            info = data.info or {}
        except Exception:
            info = {}

        if not hist.empty:
            close = float(hist["Close"].iloc[-1])
            sma50 = float(hist["Close"].tail(50).mean()) if len(hist) >= 50 else None
            sma200 = float(hist["Close"].tail(200).mean()) if len(hist) >= 200 else None

            # Calculate RSI (14)
            delta = hist["Close"].diff()
            gain = (delta.where(delta > 0, 0)).rolling(window=14).mean()
            loss = (-delta.where(delta < 0, 0)).rolling(window=14).mean()
            rs = gain / (loss + 1e-6)
            rsi_series = 100 - (100 / (1 + rs))
            rsi_val = float(rsi_series.dropna().iloc[-1]) if not rsi_series.dropna().empty else None

            pe_ratio = finite_number(info.get("trailingPE")) or finite_number(info.get("forwardPE"))
            div_yield = finite_number(info.get("dividendYield"))
            mcap = finite_number(info.get("marketCap"))

            metrics = {
                "ticker": ticker_clean,
                "close": round(close, 2),
                "sma_50": round(sma50, 2) if sma50 is not None else None,
                "sma_200": round(sma200, 2) if sma200 is not None else None,
                "rsi": round(rsi_val, 1) if rsi_val is not None else None,
                "pe_ratio": pe_ratio,
                "dividend_yield": div_yield,
                "currency": info.get('currency') or ('INR' if symbol.endswith(('.NS', '.BO')) else None),
                "asOf": hist.index[-1].isoformat(),
                "market_cap": mcap,
                "source": "yfinance Live Feed"
            }

            metrics["ai_memo"] = "Historical technical indicators from Yahoo Finance. No forward return prediction."
            return metrics
        else:
            raise ValueError("No history found for symbol")
    except Exception as e:
        print(f"yfinance metrics error for {ticker}: {e}")
        raise RuntimeError("Market indicators unavailable from provider") from e


def get_stock_chart(ticker: str, period: str = "1M") -> Dict[str, Any]:
    """
    Fetches historical price points for interactive charting using yfinance.
    """
    ticker_clean = ticker.upper().strip()
    symbol = resolve_ticker(ticker_clean)

    period_map = {"1D": "1d", "1W": "5d", "1M": "1mo", "1Y": "1y", "5Y": "5y"}
    yf_period = period_map.get(period, "1mo")

    try:
        data = yf.Ticker(symbol)
        hist = price_history(symbol, period=yf_period, interval='5m' if period == '1D' else '1d', asset=data)

        if not hist.empty:
            price_points = []
            for date, row in hist.iterrows():
                date_str = date.strftime("%b %d %H:%M" if period == '1D' else "%b %d %Y")
                close_p = float(round(row["Close"], 2))
                vol_p = finite_number(row.get("Volume"))
                price_points.append({
                    "timestamp": date_str,
                    "close": close_p,
                    "volume": vol_p,
                    "sma50": None,
                    "sma200": None,
                    "rsi": None
                })

            current_p = float(round(hist["Close"].iloc[-1], 2))
            first_p = float(round(hist["Close"].iloc[0], 2))
            price_change = current_p - first_p
            price_change_pct = (price_change / first_p) * 100.0 if first_p > 0 else 0.0

            return {
                "ticker": ticker_clean,
                "currentPrice": current_p,
                "priceChange": round(price_change, 2),
                "priceChangePercent": round(price_change_pct, 2),
                "pricePoints": price_points,
                "candleSticks": []
            }
        else:
            raise ValueError("Empty history")
    except Exception as e:
        print(f"yfinance chart error for {ticker}: {e}")
        raise RuntimeError("Historical prices unavailable from provider") from e

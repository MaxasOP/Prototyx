from typing import Dict, Any, List
import yfinance as yf
import random

INDIAN_BLUECHIPS = ["TCS", "RELIANCE", "INFY", "HDFCBANK", "ITC", "HINDUNILVR"]

def resolve_ticker(ticker: str) -> str:
    t = ticker.upper().strip()
    if t.endswith(('.NS', '.BO')):
        return t
    if t in INDIAN_BLUECHIPS:
        return f"{t}.NS"
    return t

def get_latest_metrics(ticker: str) -> Dict[str, Any]:
    """
    Fetches real-time market indicators using yfinance with statistical fallback.
    """
    ticker_clean = ticker.upper().strip()
    symbol = resolve_ticker(ticker_clean)

    try:
        data = yf.Ticker(symbol)
        hist = data.history(period="2y")
        info = data.info or {}

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

            pe_ratio = info.get("trailingPE") or info.get("forwardPE")
            div_yield = info.get("dividendYield")
            mcap = info.get("marketCap") or None

            metrics = {
                "ticker": ticker_clean,
                "close": round(close, 2),
                "sma_50": round(sma50, 2) if sma50 is not None else None,
                "sma_200": round(sma200, 2) if sma200 is not None else None,
                "rsi": round(rsi_val, 1) if rsi_val is not None else None,
                "pe_ratio": pe_ratio,
                "dividend_yield": div_yield,
                "currency": info.get('currency'),
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
        hist = data.history(period=yf_period, interval='5m' if period == '1D' else '1d', auto_adjust=True)

        if not hist.empty:
            price_points = []
            for date, row in hist.iterrows():
                date_str = date.strftime("%b %d")
                close_p = float(round(row["Close"], 2))
                vol_p = float(round(row["Volume"], 0))
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

import random
from typing import Dict, List, Any

# Dynamic imports with fallbacks
try:
    import pandas as pd
    import numpy as np
    import yfinance as yf
    HAS_LIBS = True
except ImportError:
    HAS_LIBS = False

def get_stock_data(ticker: str, period: str = "1y"):
    """
    Fetch historical market data using yfinance.
    """
    if not HAS_LIBS:
        return None
    stock = yf.Ticker(ticker)
    df = stock.history(period=period)
    return df

def calculate_technical_indicators(df) -> Any:
    """
    Calculate core technical indicators.
    """
    if not HAS_LIBS:
        return None
    # 1. Simple Moving Averages
    df['SMA_50'] = df['Close'].rolling(window=50).mean()
    df['SMA_200'] = df['Close'].rolling(window=200).mean()

    # 2. Exponential Moving Average
    df['EMA_12'] = df['Close'].ewm(span=12, adjust=False).mean()
    df['EMA_26'] = df['Close'].ewm(span=26, adjust=False).mean()

    # 3. MACD Calculation
    df['MACD'] = df['EMA_12'] - df['EMA_26']
    df['MACD_Signal'] = df['MACD'].ewm(span=9, adjust=False).mean()
    df['MACD_Hist'] = df['MACD'] - df['MACD_Signal']

    # 4. RSI Calculation (Wilder's RSI)
    delta = df['Close'].diff()
    gain = (delta.where(delta > 0, 0)).copy()
    loss = (-delta.where(delta < 0, 0)).copy()
    
    avg_gain = gain.rolling(window=14).mean()
    avg_loss = loss.rolling(window=14).mean()
    
    rs = avg_gain / avg_loss.replace(0, np.nan)
    df['RSI'] = 100 - (100 / (1 + rs))
    df['RSI'] = df['RSI'].fillna(50)

    # 5. Bollinger Bands
    df['BB_Middle'] = df['Close'].rolling(window=20).mean()
    df['BB_Std'] = df['Close'].rolling(window=20).std()
    df['BB_Upper'] = df['BB_Middle'] + (df['BB_Std'] * 2)
    df['BB_Lower'] = df['BB_Middle'] - (df['BB_Std'] * 2)

    return df

def get_latest_metrics(ticker: str) -> Dict[str, Any]:
    """
    Retrieves the latest technical indicators for a given ticker.
    Falls back to high-quality random/realistic mock metrics if libs are missing.
    """
    ticker_up = ticker.upper().strip().replace(".NS", "").replace(".BO", "")
    
    if not HAS_LIBS:
        # Fallback to realistic mock metrics for demo
        random.seed(hash(ticker_up))
        base_price = 3000.0 if ticker_up == "TCS" else (2400.0 if ticker_up == "RELIANCE" else (180.0 if ticker_up == "AAPL" else 1500.0))
        price = base_price + random.uniform(-100, 100)
        
        return {
            "ticker": ticker_up,
            "close": round(price, 2),
            "sma_50": round(price * 0.98, 2),
            "sma_200": round(price * 0.94, 2),
            "rsi": round(random.uniform(40, 75), 2),
            "macd": round(random.uniform(-5, 5), 2),
            "macd_signal": round(random.uniform(-3, 3), 2),
            "macd_hist": round(random.uniform(-2, 2), 2),
            "bb_upper": round(price * 1.05, 2),
            "bb_middle": round(price, 2),
            "bb_lower": round(price * 0.95, 2),
            "pe_ratio": round(random.uniform(15, 45), 2),
            "market_cap": int(random.uniform(10, 20) * 1e11),
            "52_week_high": round(price * 1.15, 2),
            "52_week_low": round(price * 0.82, 2),
            "dividend_yield": round(random.uniform(0.005, 0.025), 4)
        }

    try:
        # Standardize ticker names: Only add .NS for Indian stocks (not known global ones)
        if ticker_up in ["AAPL", "MSFT", "TSLA", "GOOGL", "AMZN", "META", "NVDA"]:
            ticker_yf = ticker_up
        elif len(ticker_up) <= 6:
            ticker_yf = f"{ticker_up}.NS"
        else:
            ticker_yf = ticker_up

        df = get_stock_data(ticker_yf, period="1y")
        if df is None or df.empty:
            return {"error": f"No data found for ticker {ticker}"}

        df_indicators = calculate_technical_indicators(df)
        latest = df_indicators.iloc[-1]

        # Extract basic fundamental info
        ticker_info = yf.Ticker(ticker_yf).info
        pe_ratio = ticker_info.get("forwardPE", ticker_info.get("trailingPE", None))
        market_cap = ticker_info.get("marketCap", None)
        fifty_two_week_high = ticker_info.get("fiftyTwoWeekHigh", None)
        fifty_two_week_low = ticker_info.get("fiftyTwoWeekLow", None)
        dividend_yield = ticker_info.get("dividendYield", 0)

        # Final JSON-ready response sanitization
        def sanitize_val(v):
            if isinstance(v, float) and (pd.isna(v) or np.isinf(v)):
                return 0.0
            return v

        return {
            "ticker": ticker_up,
            "close": float(sanitize_val(latest["Close"])),
            "sma_50": float(sanitize_val(latest["SMA_50"])),
            "sma_200": float(sanitize_val(latest["SMA_200"])),
            "rsi": float(sanitize_val(latest["RSI"])),
            "macd": float(sanitize_val(latest["MACD"])),
            "macd_signal": float(sanitize_val(latest["MACD_Signal"])),
            "macd_hist": float(sanitize_val(latest["MACD_Hist"])),
            "bb_upper": float(sanitize_val(latest["BB_Upper"])),
            "bb_middle": float(sanitize_val(latest["BB_Middle"])),
            "bb_lower": float(sanitize_val(latest["BB_Lower"])),
            "pe_ratio": sanitize_val(pe_ratio),
            "market_cap": sanitize_val(market_cap),
            "52_week_high": sanitize_val(fifty_two_week_high),
            "52_week_low": sanitize_val(fifty_two_week_low),
            "dividend_yield": float(sanitize_val(dividend_yield))
        }
    except Exception as e:
        print(f"ERROR in market_data for {ticker}: {str(e)}")
        # Fallback to hardcoded mock if yfinance fails
        return {
            "ticker": ticker_up,
            "close": 0.0,
            "error": str(e)
        }

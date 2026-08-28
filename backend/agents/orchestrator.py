import os
import json
import asyncio
from typing import Dict, Any, List
import yfinance as yf
from duckduckgo_search import DDGS
from groq import Groq

# API Keys
GROQ_API_KEY = os.getenv("GROQ_API_KEY")

client = Groq(api_key=GROQ_API_KEY)

async def data_analyst_worker(ticker: str) -> Dict[str, Any]:
    """
    📉 Data Analyst: Fetches Price, Volatility, and key metrics via yFinance.
    """
    print(f"DEBUG: Data Analyst working on {ticker}...")
    try:
        stock = yf.Ticker(ticker)
        info = stock.info
        history = stock.history(period="1mo")
        
        current_price = info.get("regularMarketPrice") or info.get("currentPrice")
        if not current_price and not history.empty:
            current_price = history['Close'].iloc[-1]
            
        volatility = history['Close'].pct_change().std() * (252**0.5) if not history.empty else 0
        
        return {
            "ticker": ticker,
            "price": current_price,
            "volatility": f"{volatility:.2%}",
            "market_cap": info.get("marketCap"),
            "pe_ratio": info.get("trailingPE"),
            "status": "success"
        }
    except Exception as e:
        return {"ticker": ticker, "status": "error", "message": str(e)}

async def news_researcher_worker(query: str) -> Dict[str, Any]:
    """
    📰 News Researcher: Fetches sentiment and FUD via DuckDuckGo Search.
    """
    print(f"DEBUG: News Researcher searching for '{query}'...")
    try:
        # Try a few queries to ensure we get some results
        queries = [query, query.split(" ")[0] + " price news", "crypto market sentiment"]
        all_results = []
        
        with DDGS() as ddgs:
            for q in queries:
                results = [r for r in ddgs.text(q, max_results=3)]
                all_results.extend(results)
                if len(all_results) >= 5: break
        
        if not all_results:
            return {"query": query, "status": "no_results", "news_snippets": "No recent news found."}

        snippets = "\n".join([f"- {r['title']}: {r['body']}" for r in all_results])
        
        return {
            "query": query,
            "news_snippets": snippets,
            "status": "success"
        }
    except Exception as e:
        return {"query": query, "status": "error", "message": str(e)}

async def run_consultation(user_query: str) -> Dict[str, Any]:
    """
    🧠 Manager Agent: Orchestrates the Data Analyst and News Researcher.
    Uses Groq for high-speed synthesis.
    """
    print(f"DEBUG: Manager received query: '{user_query}'")
    
    # 1. Detect Intent and Asset (Step 1: Extract Ticker)
    intent_prompt = f"""
    Analyze this user query: "{user_query}"
    Identify the main financial asset (Stock or Crypto).
    Return ONLY the ticker symbol (e.g. BTC-USD, TSLA, AAPL).
    If no clear asset, return "NONE".
    """
    
    chat_completion = client.chat.completions.create(
        messages=[{"role": "user", "content": intent_prompt}],
        model="qwen/qwen3.8-27b",
    )
    ticker = chat_completion.choices[0].message.content.strip().upper().replace(" ", "")
    
    if ticker == "NONE":
        return {"error": "Could not identify a financial asset in your query."}

    print(f"DEBUG: Manager detected asset: {ticker}")

    # 2. Parallel Execution (Step 2: Workers)
    data_task = data_analyst_worker(ticker)
    news_task = news_researcher_worker(f"{ticker} investment news sentiment")
    
    data_results, news_results = await asyncio.gather(data_task, news_task)

    # 3. Synthesis (Step 3: Final Report)
    synthesis_prompt = f"""
    Act as a Senior Investment Manager. Synthesize this data into a concise recommendation.
    
    USER QUERY: {user_query}
    
    DATA ANALYST REPORT:
    {json.dumps(data_results, indent=2)}
    
    NEWS RESEARCHER REPORT:
    {json.dumps(news_results, indent=2)}
    
    STRICT RULES:
    - Use professional, direct language.
    - NO markdown (no **, no ###, no *).
    - Provide: 
        1. STRATEGY SUMMARY
        2. RISK RATING
        3. FINAL VERDICT (BUY/SELL/HOLD/WAIT)
    - Keep it under 150 words.
    """
    
    final_completion = client.chat.completions.create(
        messages=[{"role": "user", "content": synthesis_prompt}],
        model="qwen/qwen3.8-27b",
    )
    
    final_report = final_completion.choices[0].message.content.strip()

    return {
        "asset": ticker,
        "recommendation": final_report,
        "data_summary": data_results,
        "news_found": news_results.get("status") == "success"
    }

if __name__ == "__main__":
    # Test call
    async def main():
        res = await run_consultation("Should I buy Bitcoin?")
        print("\n=== FINAL REPORT ===\n")
        print(res['recommendation'])
        
    asyncio.run(main())

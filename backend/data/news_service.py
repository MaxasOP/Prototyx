import os
import json
import uuid
from typing import List, Dict, Any, Optional
from duckduckgo_search import DDGS
import yfinance as yf
from groq import Groq
from dotenv import load_dotenv

load_dotenv()
GROQ_API_KEY = os.getenv("GROQ_API_KEY")

def fetch_live_news_sentiment(ticker: Optional[str] = None) -> List[Dict[str, Any]]:
    client = None
    if GROQ_API_KEY and GROQ_API_KEY.startswith("gsk_"):
        try:
            client = Groq(api_key=GROQ_API_KEY)
        except Exception as e:
            print(f"Groq client init error: {e}")

    query = f"{ticker} stock news sentiment financial" if ticker else "US Indian stock market economy earnings news"
    all_results = []

    try:
        with DDGS() as ddgs:
            results = list(ddgs.text(query, max_results=5))
            all_results.extend(results)
    except Exception as e:
        print(f"DDGS News fetch error: {e}")

    if not all_results and ticker:
        try:
            symbol = f"{ticker}.NS" if ticker.upper() in ["TCS", "RELIANCE", "INFY", "HDFCBANK", "ITC"] else ticker
            yt = yf.Ticker(symbol)
            yf_news = yt.news or []
            for item in yf_news[:5]:
                title = item.get("title", "")
                publisher = item.get("publisher", "Source unavailable")
                if title:
                    all_results.append({"title": title, "body": title, "source": publisher})
        except Exception as e:
            print(f"yfinance news error: {e}")

    # Default fallback articles if live search returned 0 items
    if not all_results:
        raise RuntimeError("News provider returned no articles")

    news_items = []
    for idx, item in enumerate(all_results[:4]):
        title = item.get("title", "Market Intelligence Update")
        body = item.get("body", title)
        source = item.get("source") or item.get("publisher") or item.get("href", "Source unavailable")

        sentiment = "UNASSESSED"
        sentiment_score = 0.0
        impact = "Sentiment analysis unavailable; read the source article."
        summary = title

        if client:
            try:
                prompt = f"""
                Analyze this financial news article for stock investors:
                Title: "{title}"
                Body: "{body}"

                Respond strictly in JSON format with no additional text:
                {{
                    "sentiment": "BULLISH" or "NEUTRAL" or "BEARISH",
                    "sentiment_score": float between -1.0 and 1.0,
                    "impact": "1-sentence portfolio impact analysis",
                    "summary": "1-sentence plain English summary for retail investors"
                }}
                """
                completion = client.chat.completions.create(
                    messages=[{"role": "user", "content": prompt}],
                    model=os.getenv("GROQ_MODEL", "qwen/qwen3.8-27b"),
                    response_format={"type": "json_object"}
                )
                res_data = json.loads(completion.choices[0].message.content)
                sentiment = str(res_data.get("sentiment", "NEUTRAL")).upper()
                sentiment_score = float(res_data.get("sentiment_score", 0.15))
                impact = str(res_data.get("impact", impact))
                summary = str(res_data.get("summary", summary))
            except Exception as ex:
                print(f"Groq sentiment analysis error for item {idx}: {ex}")

        news_items.append({
            "id": f"news_{idx}_{uuid.uuid4().hex[:6]}",
            "title": title,
            "source": source,
            "timestamp": "Publication time unavailable",
            "tickers": [ticker.upper()] if ticker else ["MARKET", "TECH"],
            "sentiment": sentiment,
            "sentimentScore": sentiment_score,
            "aiImpactAnalysis": impact,
            "plainEnglishSummary": summary
        })

    return news_items

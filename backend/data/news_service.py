"""Dated article feeds only. Web search results are never treated as news."""
import hashlib
from datetime import datetime, timezone, timedelta
from email.utils import parsedate_to_datetime
from functools import lru_cache
import time
import re
import requests
import xml.etree.ElementTree as ET

COMPANIES = {
    'TCS': ('Tata Consultancy Services', ['tcs','tata consultancy']),
    'RELIANCE': ('Reliance Industries', ['reliance']),
    'HDFCBANK': ('HDFC Bank', ['hdfc']),
    'INFY': ('Infosys', ['infosys','infy']),
    'AAPL': ('Apple stock', ['apple','aapl']),
    'MSFT': ('Microsoft', ['microsoft','msft']),
    'NVDA': ('Nvidia', ['nvidia','nvda']),
    'ITC': ('ITC Limited', ['itc']),
    'HINDUNILVR': ('Hindustan Unilever', ['hindustan unilever','hindunilvr']),
}

def fetch_live_news_sentiment(ticker=None):
    clean = ticker.strip().upper() if ticker else None
    return _feed(clean, int(time.time()//300))

@lru_cache(maxsize=128)
def _feed(ticker, bucket):
    query, aliases = COMPANIES.get(ticker, (ticker, [ticker.lower()] if ticker else []))
    query = (query or 'stock market economy earnings') + ' when:7d'
    response = requests.get('https://news.google.com/rss/search',
                            params={'q':query,'hl':'en-US','gl':'US','ceid':'US:en'}, timeout=15)
    response.raise_for_status()
    root = ET.fromstring(response.content)
    now = datetime.now(timezone.utc)
    items, seen = [], set()
    for article in root.findall('./channel/item'):
        title = (article.findtext('title') or '').strip()
        publisher = (article.findtext('source') or '').strip()
        url = (article.findtext('link') or '').strip()
        published = article.findtext('pubDate')
        if not title or not publisher or not url.startswith('https://') or not published:
            continue
        try:
            date = parsedate_to_datetime(published)
            if date.tzinfo is None: date = date.replace(tzinfo=timezone.utc)
        except (ValueError, TypeError):
            continue
        if not now-timedelta(days=7) <= date <= now+timedelta(minutes=5):
            continue
        if ticker and not any(re.search(r'\b'+re.escape(alias)+r'\b', title, re.IGNORECASE) for alias in aliases):
            continue
        # The source suffix is metadata, not part of the headline.
        suffix = ' - '+publisher
        headline = title[:-len(suffix)] if title.endswith(suffix) else title
        identity = headline.casefold()
        if identity in seen: continue
        seen.add(identity)
        items.append({'id':hashlib.sha256(url.encode()).hexdigest()[:20], 'title':headline,
                      'source':publisher, 'timestamp':date.isoformat(), 'url':url,
                      'tickers':[ticker] if ticker else [], 'sentiment':'UNASSESSED','sentimentScore':0.0,
                      'aiImpactAnalysis':'Publisher headline. Portfolio impact has not been assessed.',
                      'plainEnglishSummary':'Publisher headline. Open the source for the full report.'})
    items.sort(key=lambda item:item['timestamp'], reverse=True)
    return items[:5]

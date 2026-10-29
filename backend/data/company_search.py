"""Fast company discovery, separate from expensive quote/history loading."""
from functools import lru_cache
import time
import yfinance as yf

# A starting shelf, not the searchable universe. Exchange suffixes are retained.
POPULAR = [
    ('TCS', 'Tata Consultancy Services', 'NSE'), ('RELIANCE', 'Reliance Industries', 'NSE'),
    ('INFY', 'Infosys', 'NSE'), ('HDFCBANK', 'HDFC Bank', 'NSE'), ('ITC', 'ITC', 'NSE'),
    ('HINDUNILVR', 'Hindustan Unilever', 'NSE'), ('ICICIBANK.NS', 'ICICI Bank', 'NSE'),
    ('SBIN.NS', 'State Bank of India', 'NSE'), ('BHARTIARTL.NS', 'Bharti Airtel', 'NSE'),
    ('LT.NS', 'Larsen & Toubro', 'NSE'), ('SUNPHARMA.NS', 'Sun Pharmaceutical', 'NSE'),
    ('MARUTI.NS', 'Maruti Suzuki', 'NSE'), ('TITAN.NS', 'Titan Company', 'NSE'),
    ('BAJFINANCE.NS', 'Bajaj Finance', 'NSE'), ('ASIANPAINT.NS', 'Asian Paints', 'NSE'),
    ('WIPRO.NS', 'Wipro', 'NSE'), ('HCLTECH.NS', 'HCL Technologies', 'NSE'),
    ('AAPL', 'Apple', 'NASDAQ'), ('MSFT', 'Microsoft', 'NASDAQ'),
    ('NVDA', 'NVIDIA', 'NASDAQ'), ('AMZN', 'Amazon', 'NASDAQ'),
    ('GOOGL', 'Alphabet', 'NASDAQ'), ('META', 'Meta Platforms', 'NASDAQ'),
    ('TSLA', 'Tesla', 'NASDAQ'), ('NFLX', 'Netflix', 'NASDAQ'),
    ('AMD', 'Advanced Micro Devices', 'NASDAQ'), ('AVGO', 'Broadcom', 'NASDAQ'),
    ('JPM', 'JPMorgan Chase', 'NYSE'), ('V', 'Visa', 'NYSE'),
    ('WMT', 'Walmart', 'NASDAQ'), ('KO', 'Coca-Cola', 'NYSE'), ('XOM', 'Exxon Mobil', 'NYSE'),
]

def item(ticker, name, exchange, sector='', industry=''):
    return dict(ticker=ticker, name=name, exchange=exchange, sector=sector, industry=industry)

@lru_cache(maxsize=256)
def provider_search(query, bucket):
    quotes = yf.Search(query, max_results=30, news_count=0, timeout=8).quotes
    return [item(q['symbol'], q.get('longname') or q.get('shortname') or q['symbol'],
                 q.get('exchDisp') or q.get('exchange') or '', q.get('sector') or '', q.get('industry') or '')
            for q in quotes if q.get('symbol') and q.get('quoteType') == 'EQUITY']

def search_companies(query=None, sector=None):
    from data.market_data import resolve_ticker
    q = (query or '').strip().lower()[:100]
    local = [item(*row) for row in POPULAR if not q or q in row[0].lower() or q in row[1].lower()]
    remote = []
    if len(q) >= 2:
        try:
            remote = provider_search(q, int(time.time() // 300))
        except Exception as exc:
            if not local:
                raise RuntimeError('Company search is temporarily unavailable. Try again.') from exc
    merged = {}
    for company in local + remote:
        merged.setdefault(resolve_ticker(company['ticker']), company)
    results = list(merged.values())
    if sector:
        results = [c for c in results if sector.lower() in c['sector'].lower()]
    if q:
        results.sort(key=lambda c: (c['ticker'].lower().split('.')[0] != q,
                                   c['name'].lower() != q, not c['name'].lower().startswith(q),
                                   not c['ticker'].lower().startswith(q), c['exchange'] != 'NSE', c['name']))
    return results[:40]

import unittest
from unittest.mock import patch, Mock
from datetime import datetime, timezone, timedelta
from email.utils import format_datetime
from data.news_service import _feed, fetch_live_news_sentiment

class NewsTests(unittest.TestCase):
    def setUp(self): _feed.cache_clear()

    def article(self, title, days=0, source='Example Financial News', dated=True):
        date = format_datetime(datetime.now(timezone.utc)-timedelta(days=days))
        return f'<item><title>{title}</title><link>https://news.google.com/rss/articles/{days}</link><source>{source}</source>' + (f'<pubDate>{date}</pubDate>' if dated else '') + '</item>'

    def test_filters_unrelated_undated_and_stale_articles(self):
        content = '<rss><channel>'+''.join([
            self.article('Apple reports quarterly earnings'),
            self.article('United States - Wikipedia'),
            self.article('Apple old earnings', days=30),
            self.article('Apple promotional homepage', dated=False)])+'</channel></rss>'
        with patch('data.news_service.requests.get', return_value=Mock(content=content.encode())):
            results = fetch_live_news_sentiment('AAPL')
        self.assertEqual(len(results),1)
        self.assertEqual(results[0]['tickers'],['AAPL'])
        self.assertEqual(results[0]['sentiment'],'UNASSESSED')
        self.assertNotIn('TECH',results[0]['tickers'])

    def test_symbol_match_requires_word_boundaries(self):
        content = '<rss><channel>'+self.article('Bitcoin market update')+self.article('ITC reports results')+'</channel></rss>'
        with patch('data.news_service.requests.get', return_value=Mock(content=content.encode())):
            results = fetch_live_news_sentiment('ITC')
        self.assertEqual([x['title'] for x in results],['ITC reports results'])

    def test_general_news_has_no_invented_portfolio_tags(self):
        content = '<rss><channel>'+self.article('Market closes higher')+'</channel></rss>'
        with patch('data.news_service.requests.get', return_value=Mock(content=content.encode())):
            self.assertEqual(fetch_live_news_sentiment()[0]['tickers'],[])

if __name__=='__main__': unittest.main()

import unittest
from unittest.mock import patch, Mock
from data.company_search import search_companies, provider_search

class CompanySearchTests(unittest.TestCase):
    def tearDown(self):
        provider_search.cache_clear()

    def test_default_is_larger_and_does_not_download_market_data(self):
        with patch('data.company_search.yf.Search') as search:
            self.assertGreaterEqual(len(search_companies()), 30)
            search.assert_not_called()

    def test_name_query_includes_remote_company_and_retains_exchange(self):
        with patch('data.company_search.yf.Search', return_value=Mock(quotes=[
            dict(symbol='SHOP', longname='Shopify', quoteType='EQUITY', exchDisp='NASDAQ'),
            dict(symbol='SHOP.NS', longname='Shop India', quoteType='EQUITY', exchDisp='NSE'),
            dict(symbol='SHOPUSD', quoteType='CURRENCY')])):
            results = search_companies('shop')
            self.assertEqual({c['ticker'] for c in results}, {'SHOP', 'SHOP.NS'})

    def test_exact_symbol_ranks_first_and_provider_results_are_cached(self):
        with patch('data.company_search.yf.Search', return_value=Mock(quotes=[
            dict(symbol='AAPL', shortname='Apple', quoteType='EQUITY')])) as search:
            self.assertEqual(search_companies('aapl')[0]['ticker'], 'AAPL')
            self.assertEqual(len(search_companies('aapl')), 1)
            search.assert_called_once()

    def test_provider_failure_preserves_local_matches_but_reports_unknown_failure(self):
        with patch('data.company_search.yf.Search', side_effect=RuntimeError('offline')):
            self.assertTrue(any(c['ticker']=='HDFCBANK' for c in search_companies('hdfc')))
            with self.assertRaises(RuntimeError): search_companies('unknown-company')

    def test_indian_exchange_is_not_stripped_from_market_requests(self):
        import pandas as pd
        from data.market_data import get_latest_metrics, get_stock_chart
        history = pd.DataFrame({'Close': [100.,101.], 'Volume':[1,2]}, index=pd.date_range('2026-01-01', periods=2))
        ticker = Mock(info={'currency':'INR'})
        ticker.history.return_value = history
        with patch('data.market_data.yf.Ticker', return_value=ticker) as lookup:
            get_latest_metrics('SBIN.NS')
            get_stock_chart('SBIN.BO')
            self.assertEqual([call.args[0] for call in lookup.call_args_list], ['SBIN.NS','SBIN.BO'])

if __name__ == '__main__': unittest.main()

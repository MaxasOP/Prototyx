"""Production failures: missing Yahoo closes and fabricated fallback responses."""
import asyncio
import json
import os
import unittest
from unittest.mock import Mock, patch
import numpy as np
import pandas as pd
import test_integrity
from data.market_data import get_stock_chart, get_latest_metrics
from quant.history import returns_for
from agents.llm import generate_text


class MarketRegressions(unittest.TestCase):
    setUp = test_integrity.IntegrityTests.setUp
    tearDown = test_integrity.IntegrityTests.tearDown
    register = test_integrity.IntegrityTests.register

    def history(self):
        return pd.DataFrame({'Close': [100., 102., np.nan, np.inf, -1., 0.],
                             'Volume': [1., np.nan, 3., 4., 5., 6.]},
                            index=pd.date_range('2026-10-01', periods=6))

    def test_missing_closes_do_not_crash_chart_or_indicators(self):
        asset = Mock(info={'trailingPE': np.nan, 'marketCap': np.inf})
        asset.history.return_value = self.history()
        with patch('data.market_data.yf.Ticker', return_value=asset):
            for path in ['chart', 'indicators']:
                response = self.client.get('/api/market/' + path + '/TCS')
                self.assertEqual(response.status_code, 200, response.text)
                json.dumps(response.json(), allow_nan=False)
            chart = get_stock_chart('TCS')
            self.assertEqual(chart['currentPrice'], 102.)
            self.assertEqual(len(chart['pricePoints']), 2)
            self.assertIsNone(chart['pricePoints'][-1]['volume'])

    def test_optional_info_failure_does_not_hide_prices(self):
        from unittest.mock import PropertyMock
        asset = Mock()
        asset.history.return_value = self.history()
        type(asset).info = PropertyMock(side_effect=RuntimeError('metadata offline'))
        with patch('data.market_data.yf.Ticker', return_value=asset):
            self.assertEqual(get_latest_metrics('TCS')['close'], 102.)

    def test_empty_provider_history_is_explicit_unavailability(self):
        asset = Mock()
        asset.history.return_value = pd.DataFrame()
        with patch('data.market_data.yf.Ticker', return_value=asset):
            response = self.client.get('/api/market/chart/TCS')
            self.assertEqual(response.status_code, 503)
            self.assertNotIn('pricePoints', response.json())
        with patch('quant.history.price_history', side_effect=RuntimeError('offline')):
            with self.assertRaises(RuntimeError):
                returns_for(['TCS'])

    def test_invalid_analysis_request_is_not_server_error(self):
        self.assertEqual(self.client.post('/api/quant/optimize', json={'tickers': []}).status_code, 422)

    def test_gemini_failure_uses_groq(self):
        result = Mock()
        result.choices = [Mock(message=Mock(content='{"ok": true}'))]
        with patch.dict(os.environ, {'GEMINI_API_KEY': 'fixture', 'GROQ_API_KEY': 'fixture'}), \
             patch('agents.llm.requests.post', side_effect=__import__('requests').Timeout), \
             patch('agents.llm.Groq') as groq:
            groq.return_value.chat.completions.create.return_value = result
            self.assertEqual(json.loads(generate_text('fixture', True)), {'ok': True})
            groq.assert_called_once()

    def test_ai_optimize_actually_saves_weights(self):
        from agents.orchestrator import run_consultation
        from models import User, Holding
        from sqlmodel import Session, select
        auth = self.register('optimizer@example.test')
        self.client.post('/api/holdings/sync', headers=auth, json={'holdings': {'TCS': .5, 'RELIANCE': .5}})
        with Session(self.engine) as session:
            user = session.exec(select(User)).first()
            with patch('agents.orchestrator.call_llm', return_value='{"ticker":"NONE","action":"optimize"}'), \
                 patch('quant.optimizer.optimize_portfolio_mvo', return_value={'weights': {'TCS': .2, 'RELIANCE': .8}}):
                result = asyncio.run(run_consultation('optimize my portfolio', user, session))
                self.assertIn('Successfully', result['recommendation'])
            self.assertEqual({h.ticker: h.weight for h in session.exec(select(Holding)).all()}, {'TCS': .2, 'RELIANCE': .8})


if __name__ == '__main__':
    unittest.main()

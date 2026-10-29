"""Deterministic integration tests; never write to the deployed database."""
import os
import unittest
from unittest.mock import patch
os.environ['SECRET_KEY'] = 'isolated-test-key-not-for-deployment-123456789'
os.environ['DATABASE_URL'] = 'sqlite://'
import numpy as np
import pandas as pd
from fastapi.testclient import TestClient
from sqlmodel import SQLModel, Session, create_engine
from sqlalchemy.pool import StaticPool
from app import app
from database import get_session
from quant.optimizer import optimize_portfolio_mvo
from data.earnings import get_earnings_transcript
from data.screener_service import get_finny_strategy_report


class IntegrityTests(unittest.TestCase):
    def setUp(self):
        self.engine = create_engine('sqlite://', connect_args={'check_same_thread':False}, poolclass=StaticPool)
        SQLModel.metadata.create_all(self.engine)
        def session():
            with Session(self.engine) as s:
                yield s
        app.dependency_overrides[get_session] = session
        self.client = TestClient(app)

    def tearDown(self):
        app.dependency_overrides.clear()
        self.engine.dispose()

    def register(self, email):
        response = self.client.post('/api/auth/register', json={'email':email,'password':'test-password-123'})
        self.assertEqual(response.status_code,200)
        return {'Authorization':'Bearer '+response.json()['accessToken']}

    def test_auth_sync_deletion_and_account_isolation(self):
        a = self.register('a@example.test')
        b = self.register('b@example.test')
        self.assertEqual(self.client.get('/api/holdings').status_code,401)
        self.assertEqual(self.client.post('/api/auth/login',json={'email':'a@example.test','password':'wrong'}).status_code,401)
        response = self.client.post('/api/holdings/sync',headers=a,json={'holdings':{'AAPL':.4,'MSFT':.6}})
        self.assertEqual(response.json(),{'AAPL':.4,'MSFT':.6})
        self.assertEqual(self.client.get('/api/holdings',headers=b).json(),{})
        self.assertEqual(self.client.post('/api/holdings/sync',headers=a,json={'holdings':{'MSFT':1}}).json(),{'MSFT':1})
        self.assertEqual(self.client.post('/api/holdings/sync',headers=a,json={'holdings':{}}).json(),{})

    def test_invalid_allocation_does_not_mutate(self):
        auth = self.register('a@example.test')
        self.client.post('/api/holdings/sync',headers=auth,json={'holdings':{'AAPL':1}})
        self.assertEqual(self.client.post('/api/holdings/sync',headers=auth,json={'holdings':{'AAPL':-1}}).status_code,422)
        self.assertEqual(self.client.get('/api/holdings',headers=auth).json(),{'AAPL':1})

    def test_optimizer_uses_observations_and_valid_constraints(self):
        returns = pd.DataFrame({'A':[.01,-.01,.02,-.02]*30,'B':[.002,-.001,.001,-.002]*30})
        with patch('quant.optimizer.returns_for', return_value=returns):
            result = optimize_portfolio_mvo(['A','B'])
            self.assertAlmostEqual(sum(result['weights'].values()),1)
            self.assertGreater(result['weights']['B'],.9)
            self.assertTrue(all(0<=w<=1 for w in result['weights'].values()))
            with self.assertRaises(ValueError):
                optimize_portfolio_mvo(['A','B'], {'A':float('nan')})

    def test_transcript_preserves_provider_text(self):
        event = {'data':[{'fiscal_year':2026,'fiscal_quarter':3,'transcripts':[{'id':'trraw_test','type':'raw'}]}]}
        with patch('data.earnings._get', side_effect=[event,{'text':'Actual fixture transcript'}]):
            self.assertEqual(get_earnings_transcript('AAPL')['prepared_remarks'],'Actual fixture transcript')
        with patch('data.earnings._get', return_value={'data':[]}):
            with self.assertRaises(ValueError): get_earnings_transcript('AAPL')

    def test_briefing_depends_on_actual_holdings(self):
        a = get_finny_strategy_report({'A':1})
        b = get_finny_strategy_report({'A':.5,'B':.5})
        self.assertNotEqual(a['portfolioHealthScore'],b['portfolioHealthScore'])
        self.assertEqual(a['suggestedRebalance'],{})

if __name__ == '__main__':
    unittest.main()

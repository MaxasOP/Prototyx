from unittest.mock import patch
from sqlmodel import Session, select
import unittest
import test_integrity
from models import PasswordReset

class AccountTests(unittest.TestCase):
    setUp=test_integrity.IntegrityTests.setUp
    tearDown=test_integrity.IntegrityTests.tearDown
    register=test_integrity.IntegrityTests.register

    def test_signout_all_preserves_password_but_rejects_old_tokens(self):
        auth=self.register('a@example.test')
        self.assertEqual(self.client.post('/api/account/signout-all',headers=auth,json={'password':'test-password-123'}).status_code,200)
        self.assertEqual(self.client.get('/api/account',headers=auth).status_code,401)
        self.assertEqual(self.client.post('/api/auth/login',json={'email':'a@example.test','password':'test-password-123'}).status_code,200)

    def test_auth_attempt_limit(self):
        with patch('account_api.mail_ready',return_value=False):
            for _ in range(5): self.client.post('/api/auth/forgot-password',json={'email':'none@example.test'})
            self.assertEqual(self.client.post('/api/auth/forgot-password',json={'email':'none@example.test'}).status_code,429)

    def test_expired_recovery_is_rejected(self):
        self.register('a@example.test')
        with patch('account_api.mail_ready',return_value=True),patch('account_api.send_recovery') as mail:
            self.client.post('/api/auth/forgot-password',json={'email':'a@example.test'})
            code=mail.call_args.args[1]
        with Session(self.engine) as session:
            ticket=session.exec(select(PasswordReset)).first();ticket.expires_at=0;session.add(ticket);session.commit()
        self.assertEqual(self.client.post('/api/auth/reset-password',json={'code':code,'new_password':'new-password-123'}).status_code,400)
    def test_profile_and_export_exclude_security_secrets(self):
        auth=self.register('a@example.test')
        r=self.client.post('/api/account/profile',headers=auth,json={'name':'Research User'})
        self.assertEqual(r.json()['name'],'Research User')
        data=self.client.get('/api/account/export',headers=auth).json()
        self.assertEqual(data['profile']['email'],'a@example.test')
        self.assertNotIn('password',str(data))

    def test_logout_revokes_only_current_session(self):
        first=self.register('a@example.test')
        second=self.client.post('/api/auth/login',json={'email':'a@example.test','password':'test-password-123'}).json()['accessToken']
        self.assertEqual(self.client.post('/api/auth/logout',headers=first).status_code,200)
        self.assertEqual(self.client.get('/api/account',headers=first).status_code,401)
        self.assertEqual(self.client.get('/api/account',headers={'Authorization':'Bearer '+second}).status_code,200)

    def test_password_change_invalidates_old_sessions(self):
        auth=self.register('a@example.test')
        self.assertEqual(self.client.post('/api/account/password',headers=auth,json={'current_password':'wrong','new_password':'changed-password'}).status_code,400)
        self.assertEqual(self.client.post('/api/account/password',headers=auth,json={'current_password':'test-password-123','new_password':'changed-password'}).status_code,200)
        self.assertEqual(self.client.get('/api/account',headers=auth).status_code,401)
        self.assertEqual(self.client.post('/api/auth/login',json={'email':'a@example.test','password':'changed-password'}).status_code,200)

    def test_delete_requires_password_and_removes_account(self):
        auth=self.register('a@example.test')
        self.client.post('/api/holdings/sync',headers=auth,json={'holdings':{'AAPL':1}})
        self.assertEqual(self.client.post('/api/account/delete',headers=auth,json={'password':'wrong'}).status_code,400)
        self.assertEqual(self.client.post('/api/account/delete',headers=auth,json={'password':'test-password-123'}).status_code,200)
        self.assertEqual(self.client.get('/api/holdings',headers=auth).status_code,401)
        self.assertEqual(self.client.post('/api/auth/login',json={'email':'a@example.test','password':'test-password-123'}).status_code,401)

    def test_recovery_token_is_hashed_single_use_and_revokes_sessions(self):
        auth=self.register('a@example.test')
        with patch('account_api.mail_ready',return_value=True),patch('account_api.send_recovery') as mail:
            r=self.client.post('/api/auth/forgot-password',json={'email':'a@example.test'})
            self.assertEqual(r.status_code,200)
            code=mail.call_args.args[1]
            self.client.post('/api/auth/forgot-password',json={'email':'a@example.test'})
            self.assertEqual(mail.call_count,1)
        with Session(self.engine) as session:
            row=session.exec(select(PasswordReset)).first()
            self.assertNotEqual(row.code_hash,code)
        payload={'code':code,'new_password':'recovered-password'}
        self.assertEqual(self.client.post('/api/auth/reset-password',json=payload).status_code,200)
        self.assertEqual(self.client.post('/api/auth/reset-password',json=payload).status_code,400)
        self.assertEqual(self.client.get('/api/account',headers=auth).status_code,401)

    def test_recovery_and_external_pages_are_honest_when_mail_missing(self):
        with patch('account_api.mail_ready',return_value=False):
            self.assertFalse(self.client.get('/api/auth/recovery-status').json()['available'])
            self.assertEqual(self.client.post('/api/auth/forgot-password',json={'email':'unknown@example.test'}).status_code,503)
        for path in ['/legal/privacy','/legal/terms','/account/reset','/account/delete']:
            self.assertEqual(self.client.get(path).status_code,200)

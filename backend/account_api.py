"""Account controls and one-use email recovery. Secrets are never returned or logged."""
import hashlib
import os
import secrets
import smtplib
import ssl
import time
from email.message import EmailMessage
from fastapi import Depends, HTTPException
from fastapi.responses import HTMLResponse
from pydantic import BaseModel, Field
from sqlmodel import Session, select
from database import get_session
from models import User, Holding, RevokedToken, PasswordReset
from auth_utils import verify_password, get_password_hash

class ProfileEdit(BaseModel):
    name: str = Field(max_length=100)
class PasswordProof(BaseModel):
    password: str
class PasswordChange(BaseModel):
    current_password: str
    new_password: str
class RecoveryRequest(BaseModel):
    email: str = Field(max_length=254)
class RecoveryComplete(BaseModel):
    code: str = Field(min_length=32, max_length=200)
    new_password: str

def validate_password(value):
    if len(value)<8 or len(value.encode())>72:
        raise HTTPException(422, 'Password must be at least 8 characters and at most 72 UTF-8 bytes')

def mail_ready():
    return bool(os.getenv('MAIL_FROM')) and (bool(os.getenv('RESEND_API_KEY')) or all(os.getenv(k) for k in ('SMTP_HOST','SMTP_USER','SMTP_PASSWORD')))

def send_recovery(email, code):
    message = EmailMessage()
    message['From'] = os.environ['MAIL_FROM']
    message['To'] = email
    message['Subject'] = 'Reset your Prototyx password'
    base = os.getenv('PUBLIC_APP_URL','https://prototyx.onrender.com').rstrip('/')
    message.set_content(f'Reset your Prototyx password within 30 minutes:\n{base}/account/reset#{code}\n\nOr paste this recovery code in the app:\n{code}\n\nIf you did not request this, ignore this email.')
    if os.getenv('RESEND_API_KEY'):
        import requests
        result=requests.post('https://api.resend.com/emails',headers={'Authorization':'Bearer '+os.environ['RESEND_API_KEY']},
                             json={'from':os.environ['MAIL_FROM'],'to':[email],'subject':message['Subject'],'text':message.get_content()},timeout=15)
        if not result.ok: raise RuntimeError('Recovery email delivery failed')
        return
    port = int(os.getenv('SMTP_PORT','587'))
    if port == 465:
        with smtplib.SMTP_SSL(os.environ['SMTP_HOST'],port,timeout=15,context=ssl.create_default_context()) as client:
            client.login(os.environ['SMTP_USER'],os.environ['SMTP_PASSWORD']); client.send_message(message)
    else:
        with smtplib.SMTP(os.environ['SMTP_HOST'],port,timeout=15) as client:
            client.starttls(context=ssl.create_default_context())
            client.login(os.environ['SMTP_USER'],os.environ['SMTP_PASSWORD']); client.send_message(message)

def install_account_routes(app, current_user, token_dependency):
    @app.get('/api/account')
    def profile(user:User=Depends(current_user)):
        return {'email':user.email,'name':user.name}

    @app.post('/api/account/profile')
    def update_profile(request:ProfileEdit,user:User=Depends(current_user),session:Session=Depends(get_session)):
        user.name = request.name.strip() or None
        session.add(user); session.commit()
        return {'email':user.email,'name':user.name}

    @app.post('/api/auth/logout')
    def logout(token:str=Depends(token_dependency),user:User=Depends(current_user),session:Session=Depends(get_session)):
        session.add(RevokedToken(token_hash=hashlib.sha256(token.encode()).hexdigest(),user_id=user.id))
        session.commit()
        return {'message':'Signed out'}

    @app.post('/api/account/password')
    def change_password(request:PasswordChange,user:User=Depends(current_user),session:Session=Depends(get_session)):
        if not verify_password(request.current_password,user.hashed_password):
            raise HTTPException(400,'Current password is incorrect')
        validate_password(request.new_password)
        user.hashed_password = get_password_hash(request.new_password)
        session.add(user); session.commit()
        return {'message':'Password changed. Sign in again on all devices.'}

    @app.post('/api/account/signout-all')
    def signout_all(request:PasswordProof,user:User=Depends(current_user),session:Session=Depends(get_session)):
        if not verify_password(request.password,user.hashed_password): raise HTTPException(400,'Password is incorrect')
        # Re-salting the same password rotates its credential version without changing the password.
        user.hashed_password=get_password_hash(request.password)
        session.add(user);session.commit()
        return {'message':'All sessions signed out'}

    @app.get('/api/account/export')
    def export(user:User=Depends(current_user),session:Session=Depends(get_session)):
        holdings = session.exec(select(Holding).where(Holding.user_id==user.id)).all()
        return {'profile':{'email':user.email,'name':user.name},'holdings':{h.ticker:h.weight for h in holdings},'exportedAt':time.time()}

    @app.post('/api/account/delete')
    def delete(request:PasswordProof,user:User=Depends(current_user),session:Session=Depends(get_session)):
        if not verify_password(request.password,user.hashed_password):
            raise HTTPException(400,'Password is incorrect')
        for model in (Holding,RevokedToken,PasswordReset):
            for row in session.exec(select(model).where(model.user_id==user.id)).all(): session.delete(row)
        session.delete(user); session.commit()
        return {'message':'Account and associated application data deleted'}

    @app.get('/api/auth/recovery-status')
    def recovery_status(): return {'available':mail_ready()}

    @app.post('/api/auth/forgot-password')
    def forgot(request:RecoveryRequest,session:Session=Depends(get_session)):
        if not mail_ready(): raise HTTPException(503,'Password recovery email is not configured. Contact the app operator.')
        user = session.exec(select(User).where(User.email==request.email.strip().lower()).with_for_update()).first()
        if user:
            recent = session.exec(select(PasswordReset).where(PasswordReset.user_id==user.id,PasswordReset.created_at>time.time()-300)).first()
            if not recent:
                code = secrets.token_urlsafe(32)
                session.add(PasswordReset(code_hash=hashlib.sha256(code.encode()).hexdigest(),user_id=user.id,expires_at=time.time()+1800,created_at=time.time()))
                session.commit()
                try: send_recovery(user.email,code)
                except Exception: pass  # Never disclose account existence or provider credentials.
        return {'message':'If that account exists, a recovery email will arrive shortly.'}

    @app.post('/api/auth/reset-password')
    def reset(request:RecoveryComplete,session:Session=Depends(get_session)):
        ticket = session.exec(select(PasswordReset).where(PasswordReset.code_hash==hashlib.sha256(request.code.encode()).hexdigest()).with_for_update()).first()
        if not ticket or ticket.used or ticket.expires_at<time.time(): raise HTTPException(400,'Recovery code is invalid or expired')
        user = session.get(User,ticket.user_id)
        if not user: raise HTTPException(400,'Recovery code is invalid or expired')
        validate_password(request.new_password)
        user.hashed_password=get_password_hash(request.new_password)
        for row in session.exec(select(PasswordReset).where(PasswordReset.user_id==user.id)).all():
            row.used=True; session.add(row)
        session.add(user); session.commit()
        return {'message':'Password reset. Sign in with your new password.'}

    @app.get('/legal/privacy',response_class=HTMLResponse)
    def privacy():
        return page('Privacy & data','Prototyx stores your account email, display name, password hash and allocation weights. Session and recovery records support account security. Research queries and selected company data may be sent to Groq, Yahoo Finance, ROIC.ai and news providers. Local settings and notification history stay on your device. Export or delete your account in Account & settings. Deletion removes active application records; infrastructure logs and backups may persist under the hosting provider’s retention. Do not submit sensitive financial identifiers in research prompts.')

    @app.get('/legal/terms',response_class=HTMLResponse)
    def terms():
        return page('About this research service','Prototyx is a research and allocation-planning tool. It does not execute trades or guarantee returns. Provider data may be delayed, incomplete or unavailable. Historical analysis and generated research are not personalized investment advice. Verify source documents before making decisions. You control your allocation and notification preferences.')

    @app.get('/account/reset',response_class=HTMLResponse)
    def reset_page():
        return page('Reset password', '<form id="f"><label>Recovery code<input id="code" required></label><label>New password<input id="password" type="password" minlength="8" required></label><button>Reset password</button></form><p id="result" role="status"></p><script>document.getElementById("code").value=location.hash.slice(1);history.replaceState(null,"",location.pathname);document.getElementById("f").onsubmit=async e=>{e.preventDefault();let r=await fetch("/api/auth/reset-password",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({code:document.getElementById("code").value,new_password:document.getElementById("password").value})});let d=await r.json();document.getElementById("result").textContent=d.message||d.detail||"Request failed";};</script>',raw=True)

    @app.get('/account/delete',response_class=HTMLResponse)
    def deletion_page():
        return page('Delete your Prototyx account','<p>This permanently deletes your profile, allocations and security records from the active application database. Infrastructure logs and backups may persist under provider retention. This cannot be undone.</p><form id="f"><label>Email<input id="email" type="email" required></label><label>Password<input id="password" type="password" required></label><label><input id="confirm" type="checkbox" required> I understand this permanently deletes my account.</label><button>Delete account and data</button></form><p id="result" role="status"></p><script>document.getElementById("f").onsubmit=async e=>{e.preventDefault();let password=document.getElementById("password").value;let r=await fetch("/api/auth/login",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({email:document.getElementById("email").value,password})});let d=await r.json();if(r.ok){r=await fetch("/api/account/delete",{method:"POST",headers:{"Content-Type":"application/json","Authorization":"Bearer "+d.accessToken},body:JSON.stringify({password})});d=await r.json();}document.getElementById("result").textContent=d.message||d.detail||"Request failed";document.getElementById("password").value="";};</script>',raw=True)

def page(title,body,raw=False):
    import html
    content = body if raw else '<p>'+html.escape(body)+'</p>'
    return '<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="referrer" content="no-referrer"><title>Prototyx — '+title+'</title><style>body{max-width:640px;margin:48px auto;padding:24px;background:#f6f3eb;color:#252923;font:17px/1.6 sans-serif}h1{font-family:serif}label{display:block;margin:18px 0}input:not([type=checkbox]){display:block;width:95%;padding:12px}button{background:#344d35;color:white;border:0;padding:16px;cursor:pointer}</style><h1>'+title+'</h1>'+content+'</html>'

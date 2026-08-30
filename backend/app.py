import uvicorn
from fastapi import FastAPI, HTTPException, Depends, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import OAuth2PasswordBearer
from sqlmodel import Session, select
from typing import List, Dict, Optional
import os

from database import init_db, get_session
from models import (
    User, Holding, LoginRequest, RegisterRequest, AuthResponse,
    SyncHoldingsRequest, RiskMeshRequest, OptimizeRequest,
    TickerListRequest, ConsultRequest
)
from auth_utils import (
    get_password_hash, verify_password, create_access_token, decode_access_token
)

app = FastAPI(
    title="Prototyx AI Wealth Terminal",
    description="Secure Multi-Agent Wealth Intelligence",
    version="2.1.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="api/auth/login")

@app.on_event("startup")
def on_startup():
    init_db()

# --- Auth Dependency ---
async def get_current_user(token: str = Depends(oauth2_scheme), session: Session = Depends(get_session)):
    payload = decode_access_token(token)
    if payload is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    email: str = payload.get("sub")
    if email is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Could not validate credentials",
        )
    user = session.exec(select(User).where(User.email == email)).first()
    if user is None:
        raise HTTPException(status_code=404, detail="User not found")
    return user

# --- Auth Endpoints ---

@app.post("/api/auth/register", response_model=AuthResponse)
def register(request: RegisterRequest, session: Session = Depends(get_session)):
    # Strip whitespace from email to avoid login issues
    email = request.email.strip().lower()
    
    # Check if user exists
    existing_user = session.exec(select(User).where(User.email == email)).first()
    if existing_user:
        raise HTTPException(status_code=400, detail="Email already registered")
    
    new_user = User(
        email=email,
        hashed_password=get_password_hash(request.password),
        name=request.name
    )
    session.add(new_user)
    session.commit()
    session.refresh(new_user)
    
    access_token = create_access_token(data={"sub": new_user.email})
    return AuthResponse(accessToken=access_token, userEmail=new_user.email)

@app.post("/api/auth/login", response_model=AuthResponse)
def login(request: LoginRequest, session: Session = Depends(get_session)):
    email = request.email.strip().lower()
    user = session.exec(select(User).where(User.email == email)).first()
    if not user or not verify_password(request.password, user.hashed_password):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    
    access_token = create_access_token(data={"sub": user.email})
    return AuthResponse(accessToken=access_token, userEmail=user.email)

# --- Holdings Sync ---

@app.post("/api/holdings/sync")
def sync_holdings(
    request: SyncHoldingsRequest, 
    current_user: User = Depends(get_current_user),
    session: Session = Depends(get_session)
):
    # This is a merge-sync logic:
    # 1. Update/Insert local holdings sent by client
    # 2. Return all holdings for this user as truth
    
    for ticker, weight in request.holdings.items():
        existing_holding = session.exec(
            select(Holding).where(Holding.user_id == current_user.id, Holding.ticker == ticker)
        ).first()
        
        if weight <= 0:
            if existing_holding:
                session.delete(existing_holding)
        else:
            if existing_holding:
                existing_holding.weight = weight
            else:
                new_holding = Holding(ticker=ticker, weight=weight, user_id=current_user.id)
                session.add(new_holding)
    
    session.commit()
    
    # Refresh to get all
    all_holdings = session.exec(select(Holding).where(Holding.user_id == current_user.id)).all()
    return {h.ticker: h.weight for h in all_holdings}

# --- Market & Intelligence (Wrapped existing logic) ---

def get_indicators_local(ticker: str):
    from data.market_data import get_latest_metrics
    return get_latest_metrics(ticker)

@app.get("/api/market/indicators/{ticker}")
def get_indicators(ticker: str):
    try:
        return get_indicators_local(ticker)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/earnings/transcript/{ticker}")
def get_transcript(ticker: str, year: int = 2026, quarter: int = 3):
    from data.earnings import get_earnings_transcript
    try:
        return get_earnings_transcript(ticker, year, quarter)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/quant/risk-mesh")
def get_risk_mesh(request: RiskMeshRequest):
    from quant.risk_mesh import calculate_exposure_mesh
    try:
        return calculate_exposure_mesh(request.tickers, request.weights)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/quant/optimize")
def get_optimization(request: OptimizeRequest):
    from quant.optimizer import optimize_portfolio_mvo
    try:
        return optimize_portfolio_mvo(request.tickers, request.views)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/agents/debate")
def run_debate(request: TickerListRequest):
    from agents.committee import run_committee_debate
    try:
        return run_committee_debate(request.tickers)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/agents/consult")
async def run_consult(request: ConsultRequest):
    from agents.orchestrator import run_consultation
    try:
        return await run_consultation(request.query)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/")
def read_root():
    return {"status": "online", "secured": True}

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run("app:app", host="0.0.0.0", port=port, reload=False)

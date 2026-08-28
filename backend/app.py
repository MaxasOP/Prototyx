import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Dict, Optional, Any
from dotenv import load_dotenv
import os

# Load environment variables (mostly for consistency, though we are local-first now)
load_dotenv()

# --- Simplified Data Access (Local LLM & Archive Only) ---
def get_indicators_local(ticker: str):
    from data.market_data import get_latest_metrics
    return get_latest_metrics(ticker)

def get_transcript_local(ticker: str, year: int, quarter: int):
    from data.earnings import get_earnings_transcript
    return get_earnings_transcript(ticker, year, quarter)

def calculate_exposure_mesh_local(tickers: List[str], weights: List[float]):
    from quant.risk_mesh import calculate_exposure_mesh
    return calculate_exposure_mesh(tickers, weights)

def optimize_portfolio_local(tickers: List[str], views: Optional[Dict[str, float]]):
    from quant.optimizer import optimize_portfolio_mvo
    return optimize_portfolio_mvo(tickers, views)

def run_debate_local(tickers: List[str]):
    from agents.committee import run_committee_debate
    return run_committee_debate(tickers)

async def run_consult_local(query: str):
    from agents.orchestrator import run_consultation
    return await run_consultation(query)

app = FastAPI(
    title="Prototyx Local AI Terminal",
    description="Offline-First Multi-Agent Wealth Intelligence Powered by Llama 3.1",
    version="2.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Pydantic Schemas ---
class TickerListRequest(BaseModel):
    tickers: List[str]

class RiskMeshRequest(BaseModel):
    tickers: List[str]
    weights: List[float]

class OptimizeRequest(BaseModel):
    tickers: List[str]
    views: Optional[Dict[str, float]] = None

class ConsultRequest(BaseModel):
    query: str

@app.get("/")
def read_root():
    return {
        "status": "online",
        "mode": "Local-Only (Ollama)",
        "llm": "Llama 3.1"
    }

@app.get("/api/market/indicators/{ticker}")
def get_indicators(ticker: str):
    try:
        return get_indicators_local(ticker)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/earnings/transcript/{ticker}")
def get_transcript(ticker: str, year: int = 2026, quarter: int = 3):
    try:
        return get_transcript_local(ticker, year, quarter)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/quant/risk-mesh")
def get_risk_mesh(request: RiskMeshRequest):
    try:
        return calculate_exposure_mesh_local(request.tickers, request.weights)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/quant/optimize")
def get_optimization(request: OptimizeRequest):
    try:
        return optimize_portfolio_local(request.tickers, request.views)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/agents/debate")
def run_debate(request: TickerListRequest):
    try:
        return run_debate_local(request.tickers)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/api/agents/consult")
async def run_consult(request: ConsultRequest):
    try:
        return await run_consult_local(request.query)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Paytm endpoints removed as per user request to be API-free.

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run("app:app", host="0.0.0.0", port=port, reload=False)

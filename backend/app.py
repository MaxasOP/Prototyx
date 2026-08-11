import uvicorn
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Dict, Optional, Any
from dotenv import load_dotenv
import os

# Load environment variables from .env file
load_dotenv()

# --- Lazy/Conditional Imports for Quant/AI modules ---
# These modules might fail to import if dependencies (like PyPortfolioOpt or CrewAI)
# fail to install on newer Python versions (e.g., 3.14).

def get_indicators_safe(ticker: str):
    from data.market_data import get_latest_metrics
    return get_latest_metrics(ticker)

def get_transcript_safe(ticker: str, year: int, quarter: int):
    from data.earnings import get_earnings_transcript
    return get_earnings_transcript(ticker, year, quarter)

def calculate_exposure_mesh_safe(tickers: List[str], weights: List[float]):
    from quant.risk_mesh import calculate_exposure_mesh
    return calculate_exposure_mesh(tickers, weights)

def optimize_portfolio_safe(tickers: List[str], views: Optional[Dict[str, float]]):
    from quant.optimizer import optimize_portfolio_black_litterman, optimize_portfolio_mvo
    if views:
        return optimize_portfolio_black_litterman(tickers, views)
    return optimize_portfolio_mvo(tickers)

def run_debate_safe(tickers: List[str]):
    from agents.committee import run_committee_debate
    return run_committee_debate(tickers)

app = FastAPI(
    title="Prototyx AI Wealth Backend",
    description="Enterprise Multi-Agent Investment Advisory & Quantitative Analysis Engine.",
    version="1.0.0"
)

# Enable CORS for frontend/Android integrations
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

# --- Mock Ticker search using FinanceDatabase sector tags ---
MOCK_DATABASE = [
    {"ticker": "TCS", "name": "Tata Consultancy Services Ltd", "sector": "Technology", "industry": "IT Services", "exchange": "NSE"},
    {"ticker": "INFY", "name": "Infosys Ltd", "sector": "Technology", "industry": "IT Services", "exchange": "NSE"},
    {"ticker": "RELIANCE", "name": "Reliance Industries Ltd", "sector": "Energy & Retail", "industry": "Conglomerate", "exchange": "NSE"},
    {"ticker": "HDFCBANK", "name": "HDFC Bank Ltd", "sector": "Financial Services", "industry": "Banking", "exchange": "NSE"},
    {"ticker": "ICICIBANK", "name": "ICICI Bank Ltd", "sector": "Financial Services", "industry": "Banking", "exchange": "NSE"},
    {"ticker": "AAPL", "name": "Apple Inc.", "sector": "Technology", "industry": "Consumer Electronics", "exchange": "NASDAQ"},
    {"ticker": "MSFT", "name": "Microsoft Corp.", "sector": "Technology", "industry": "Software", "exchange": "NASDAQ"},
    {"ticker": "TSLA", "name": "Tesla Inc.", "sector": "Automotive", "industry": "Electric Vehicles", "exchange": "NASDAQ"}
]

@app.get("/")
def read_root():
    return {
        "status": "online",
        "app_name": "Prototyx Core Backend Engine",
        "supported_apis": [
            "/api/tickers/search",
            "/api/market/indicators",
            "/api/earnings/transcript",
            "/api/quant/risk-mesh",
            "/api/quant/optimize",
            "/api/agents/debate"
        ]
    }

@app.get("/api/tickers/search")
def search_tickers(query: Optional[str] = None, sector: Optional[str] = None):
    """
    Search and filter tickers in the universe.
    """
    results = MOCK_DATABASE
    if query:
        q = query.upper()
        results = [x for x in results if q in x["ticker"] or q in x["name"].upper()]
    if sector:
        s = sector.upper()
        results = [x for x in results if s in x["sector"].upper()]
    return results

@app.get("/api/market/indicators/{ticker}")
def get_indicators(ticker: str):
    """
    Fetch technical indicators for a ticker.
    """
    metrics = get_indicators_safe(ticker)
    if "error" in metrics:
        raise HTTPException(status_code=404, detail=metrics["error"])
    return metrics

@app.get("/api/earnings/transcript/{ticker}")
def get_transcript(ticker: str, year: int = 2026, quarter: int = 3):
    """
    Fetch corporate earnings transcripts.
    """
    transcript = get_transcript_safe(ticker, year, quarter)
    return transcript

@app.post("/api/quant/risk-mesh")
def get_risk_mesh(request: RiskMeshRequest):
    """
    Calculate cross-asset correlations and net exposures.
    """
    mesh = calculate_exposure_mesh_safe(request.tickers, request.weights)
    if "error" in mesh:
        raise HTTPException(status_code=400, detail=mesh["error"])
    return mesh

@app.post("/api/quant/optimize")
def get_optimization(request: OptimizeRequest):
    """
    Calculate portfolio optimization weights.
    """
    result = optimize_portfolio_safe(request.tickers, request.views)
    return result

@app.post("/api/agents/debate")
def run_debate(request: TickerListRequest):
    """
    Orchestrates the Investment Committee debate and returns implied return views.
    """
    debate_result = run_debate_safe(request.tickers)
    return debate_result

if __name__ == "__main__":
    uvicorn.run("app:app", host="0.0.0.0", port=8000, reload=True)

from typing import List, Dict, Any, Optional
import random

def search_screener_items(query: Optional[str] = None, risk_level: Optional[str] = None) -> List[Dict[str, Any]]:
    items = [
        {
            "ticker": "TCS",
            "name": "Tata Consultancy Services",
            "sector": "Technology",
            "price": 4250.75,
            "changePercent": 1.45,
            "peRatio": 28.5,
            "dividendYield": 2.1,
            "rsi": 58.2,
            "riskLevel": "SAFE",
            "aiAnalysis": "High quality cash flows with industry-leading ROE (38%). Safe anchor for core portfolios.",
            "plainEnglishSummary": "Very safe, top-tier company with strong profits and reliable dividends."
        },
        {
            "ticker": "RELIANCE",
            "name": "Reliance Industries Ltd",
            "sector": "Energy & Conglomerate",
            "price": 2940.10,
            "changePercent": 0.85,
            "peRatio": 24.2,
            "dividendYield": 1.2,
            "rsi": 52.0,
            "riskLevel": "BALANCED",
            "aiAnalysis": "Diversified cash flows across Telecom, Retail, and New Energy sectors.",
            "plainEnglishSummary": "Solid mega-cap company balanced across phone services, retail stores, and energy."
        },
        {
            "ticker": "NVDA",
            "name": "NVIDIA Corporation",
            "sector": "Semiconductors & AI",
            "price": 135.20,
            "changePercent": 3.12,
            "peRatio": 42.1,
            "dividendYield": 0.1,
            "rsi": 68.5,
            "riskLevel": "GROWTH",
            "aiAnalysis": "Dominant CUDA ecosystem moat with 85%+ market share in AI data center accelerators.",
            "plainEnglishSummary": "High-growth global leader in AI chips with massive enterprise demand."
        },
        {
            "ticker": "AAPL",
            "name": "Apple Inc",
            "sector": "Consumer Tech",
            "price": 188.50,
            "changePercent": -0.32,
            "peRatio": 31.0,
            "dividendYield": 0.6,
            "rsi": 48.5,
            "riskLevel": "GROWTH",
            "aiAnalysis": "Strong brand moat and recurring ecosystem subscription revenue.",
            "plainEnglishSummary": "High growth tech giant with loyal customers and global ecosystem."
        }
    ]

    if query:
        q = query.lower()
        items = [i for i in items if q in i["ticker"].lower() or q in i["name"].lower() or q in i["sector"].lower()]

    if risk_level:
        rl = risk_level.upper()
        items = [i for i in items if i["riskLevel"] == rl]

    return items

def search_tickers_catalog(query: Optional[str] = None, sector: Optional[str] = None) -> List[Dict[str, Any]]:
    catalog = [
        {"ticker": "TCS", "name": "Tata Consultancy Services", "sector": "Technology", "industry": "IT Services", "exchange": "NSE"},
        {"ticker": "RELIANCE", "name": "Reliance Industries Ltd", "sector": "Energy", "industry": "Oil & Gas Conglomerate", "exchange": "NSE"},
        {"ticker": "NVDA", "name": "NVIDIA Corporation", "sector": "Technology", "industry": "Semiconductors", "exchange": "NASDAQ"},
        {"ticker": "AAPL", "name": "Apple Inc", "sector": "Technology", "industry": "Consumer Electronics", "exchange": "NASDAQ"},
        {"ticker": "MSFT", "name": "Microsoft Corporation", "sector": "Technology", "industry": "Software & Cloud", "exchange": "NASDAQ"},
        {"ticker": "INFY", "name": "Infosys Ltd", "sector": "Technology", "industry": "IT Services", "exchange": "NSE"}
    ]

    if query:
        q = query.lower()
        catalog = [c for c in catalog if q in c["ticker"].lower() or q in c["name"].lower()]

    return catalog

def calculate_macro_stress_test(tickers: List[str], weights: List[float], scenario_id: str) -> Dict[str, Any]:
    if scenario_id == "rate_hike":
        return {
            "scenarioName": "Federal Reserve Rate Hike (+50 bps)",
            "portfolioReturnChange": -0.042,
            "initialVaR": 0.085,
            "stressedVaR": 0.118,
            "initialDrawdown": -0.062,
            "stressedDrawdown": -0.098,
            "aiHedgingStrategy": "Increase allocation in short-duration treasury cash buffers and defensive dividend payers.",
            "plainEnglishVerdict": "In a rate-hike shock, your portfolio would experience a mild ~4.2% dip. Adding dividend stocks will keep it safe!"
        }
    elif scenario_id == "oil_spike":
        return {
            "scenarioName": "Crude Oil Price Shock (+25%)",
            "portfolioReturnChange": -0.028,
            "initialVaR": 0.085,
            "stressedVaR": 0.102,
            "initialDrawdown": -0.062,
            "stressedDrawdown": -0.084,
            "aiHedgingStrategy": "Maintain overweight stance on energy sector hedges to offset input cost spikes.",
            "plainEnglishVerdict": "If oil prices jump, energy holdings will cushion the blow, keeping portfolio losses below 3%."
        }
    else:
        return {
            "scenarioName": "Tech Sector Correction (-15%)",
            "portfolioReturnChange": -0.068,
            "initialVaR": 0.085,
            "stressedVaR": 0.135,
            "initialDrawdown": -0.062,
            "stressedDrawdown": -0.124,
            "aiHedgingStrategy": "Trim high P/E tech exposure by 5% and rotate into consumer staples.",
            "plainEnglishVerdict": "A tech sell-off would impact tech holdings. FINNY recommends rotating 5% into safe consumer stocks."
        }

def get_finny_strategy_report() -> Dict[str, Any]:
    return {
        "portfolioHealthScore": 88,
        "riskStatus": "SAFE",
        "keyInsights": [
            "Portfolio is 88% optimized against systematic risk.",
            "No single stock exceeds 35% weight limit.",
            "FINNY has detected steady accumulation in Tech & Conglomerate sectors."
        ],
        "suggestedRebalance": {"TCS": 0.35, "RELIANCE": 0.30, "AAPL": 0.20, "NVDA": 0.15},
        "selfLearnedMarketRegime": "Low Volatility Accumulation Phase",
        "dailyBriefingSummary": "Hello! I am FINNY, your AI portfolio manager. Today your portfolio health is a strong 88/100. Markets are stable, and your tech holdings are benefiting from strong cloud earnings!"
    }

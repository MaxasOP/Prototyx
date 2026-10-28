import os
from typing import Dict, Any

def get_earnings_transcript(ticker: str, year: int = 2026, quarter: int = 3) -> Dict[str, Any]:
    """
    Retrieves the earnings call transcript synthesis via AI Ingestion.
    """
    ticker_clean = ticker.upper().strip().replace(".NS", "").replace(".BO", "")
    
    # Prioritize Live API if user has a key, otherwise fallback to AI Synthesis
    try:
        import earningscall
        if os.environ.get("EARNINGSCALL_API_KEY"):
            company = earningscall.get_company(ticker_clean.lower())
            transcript = company.get_transcript(year=year, quarter=quarter)
            text = transcript.text
            data = {
                "ticker": ticker_clean,
                "company_name": company.name if hasattr(company, "name") else ticker_clean,
                "quarter": f"Q{quarter} {year}",
                "prepared_remarks": text[:5000],
                "qa_session": "Q&A parsed in full summary."
            }
            from agents.committee import summarize_earnings_llm
            return summarize_earnings_llm(ticker_clean, data)
    except: pass

    # AI-Ingestion Synthesis (Real-world knowledge based)
    from agents.committee import generate_earnings_transcript_llm
    return generate_earnings_transcript_llm(ticker_clean, year, quarter)

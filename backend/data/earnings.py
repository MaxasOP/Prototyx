import os
from typing import Dict, Any

# Mock transcripts database for demonstration/offline purposes
MOCK_TRANSCRIPTS = {
    "TCS": {
        "ticker": "TCS",
        "company_name": "Tata Consultancy Services Ltd",
        "quarter": "Q1 2027",
        "prepared_remarks": (
            "Good afternoon everyone, and welcome to the TCS earnings call for Q1 FY27. "
            "We have started the year on a strong note, with double-digit revenue growth driven by "
            "sustained demand for cloud migration and generative AI integration services. "
            "Our operating margin stood resilient at 24.7% despite wage hikes. We are seeing strong "
            "traction in our cognitive business operations and cybersecurity segments. "
            "We signed several large deals this quarter, representing a total contract value of $10.2 billion. "
            "Looking forward, we remain confident in our service-led growth model and expect margins to expand "
            "as AI automation offsets resource costs."
        ),
        "qa_session": (
            "Analyst (Citi): Congratulations on the quarter. Can you speak to the pricing pressure in North America? "
            "\nManagement: Thank you. Pricing has stabilized. Clients are optimizing legacy spends but reallocating those budgets "
            "directly into GenAI pilots. "
            "\nAnalyst (Morgan Stanley): How are you addressing talent costs for AI-skilled engineers? "
            "\nManagement: We have successfully upskilled over 350,000 employees in generative AI capabilities, which has minimized "
            "our reliance on expensive external hiring."
        )
    },
    "RELIANCE": {
        "ticker": "RELIANCE",
        "company_name": "Reliance Industries Ltd",
        "quarter": "Q1 2027",
        "prepared_remarks": (
            "Good morning. Reliance Industries has recorded robust consolidated EBITDA growth this quarter. "
            "Our retail segment saw footprint expansion with digital commerce representing 18% of sales. "
            "In telecom, Jio continues its leading market share, hitting 490 million subscribers with 5G services "
            "now fully monetized through tiered data plans. "
            "In our Oil-to-Chemicals (O2C) segment, throughput was stable despite global margin pressure. "
            "We are accelerating our Green Energy investments with the Jamnagar giga-factory complex on track "
            "for production trials later this year."
        ),
        "qa_session": (
            "Analyst (JP Morgan): Can you update us on the timeline for Jio and Retail IPOs? "
            "\nManagement: We do not comment on market speculation. Our focus remains on scaling operations and generating free cash flow. "
            "\nAnalyst (Nomura): What are your capital expenditure projections for Green Energy over the next fiscal? "
            "\nManagement: We expect green energy capex to ramp up to $4.5 billion as we complete the solar cell and battery lines."
        )
    },
    "AAPL": {
        "ticker": "AAPL",
        "company_name": "Apple Inc.",
        "quarter": "Q3 2026",
        "prepared_remarks": (
            "Welcome, everyone. We are pleased to report a new June quarter revenue record of $89.6 billion, "
            "up 5% year-over-year. Services revenue reached an all-time high of $25.2 billion, fueled by paid subscriptions "
            "surpassing 1 billion. Apple Intelligence has officially rolled out to developers, and the customer reception "
            "has been outstanding. We are embedding privacy-focused generative AI directly at the chip level "
            "across iPhone, iPad, and Mac, creating a compelling upgrade cycle as we enter the fall."
        ),
        "qa_session": (
            "Analyst (Goldman Sachs): How should we think about gross margins with the rollout of Apple Intelligence? "
            "\nManagement: Our gross margins remain strong, in the range of 45-46%. The higher mix of Services offsets "
            "initial silicon ramp costs for private cloud compute. "
            "\nAnalyst (Barclays): Are you seeing carrier promotions return in North America? "
            "\nManagement: Demand remains healthy. The value proposition of Apple Intelligence is the primary driver, rather "
            "than promotional discount dynamics."
        )
    }
}

def get_earnings_transcript(ticker: str, year: int = 2026, quarter: int = 3) -> Dict[str, Any]:
    """
    Retrieves the earnings call transcript for a given ticker.
    Attempts to pull from the 'earningscall' API, and falls back to mock data
    if offline, missing API keys, or if requesting Indian tickers (which aren't on the global API).
    """
    ticker_clean = ticker.upper().replace(".NS", "").replace(".BO", "")
    
    # Check if we have mock data first to speed up local presentations
    if ticker_clean in MOCK_TRANSCRIPTS:
        return MOCK_TRANSCRIPTS[ticker_clean]

    try:
        import earningscall
        
        # Check if API token is configured
        if not os.environ.get("EARNINGSCALL_API_KEY"):
            # Return generic mock if API key is missing
            return create_generic_mock(ticker_clean, year, quarter)
            
        company = earningscall.get_company(ticker_clean.lower())
        transcript = company.get_transcript(year=year, quarter=quarter)
        
        # Parse into prepared remarks and Q&A if possible
        text = transcript.text
        prepared_remarks = text
        qa_session = "Interactive Q&A text available on full API."
        
        if "Q&A" in text:
            parts = text.split("Q&A")
            prepared_remarks = parts[0]
            qa_session = parts[1]
            
        return {
            "ticker": ticker_clean,
            "company_name": company.name if hasattr(company, "name") else ticker_clean,
            "quarter": f"Q{quarter} {year}",
            "prepared_remarks": prepared_remarks[:3000] + "\n[Truncated for Demo]...",
            "qa_session": qa_session[:2000] + "\n[Truncated for Demo]..."
        }
    except Exception as e:
        # Fallback to generic mock on any exception
        return create_generic_mock(ticker_clean, year, quarter)

def create_generic_mock(ticker: str, year: int, quarter: int) -> Dict[str, Any]:
    """
    Creates a high-quality, realistic generic mock transcript for presentation purposes.
    """
    return {
        "ticker": ticker,
        "company_name": f"{ticker} Inc.",
        "quarter": f"Q{quarter} {year}",
        "prepared_remarks": (
            f"Thank you for joining the {ticker} earnings conference call. "
            f"For the quarter, we achieved total revenue of $14.5 billion, a growth of 8% year-over-year. "
            f"We have continued to expand our operational margins through technology integrations and administrative optimizations. "
            f"We are investing heavily in our core digital transformation tools. Customer acquisition costs have decreased by 12% "
            f"this quarter, and net retention rate remains stable at 112%. We are pleased to increase our shareholder dividend "
            f"this quarter as a reflection of our strong free cash flow and balance sheet health."
        ),
        "qa_session": (
            "Analyst (Bank of America): Can you discuss your margin expansion goals? "
            "\nManagement: We expect operating margins to improve by 100-150 basis points over the next fiscal year, "
            "primarily driven by automated cloud efficiency improvements. "
            "\nAnalyst (Deutsche Bank): How is the new product line performing? "
            "\nManagement: It is exceeding expectations, contributing 4% to total revenue in its first full quarter of launch."
        )
    }

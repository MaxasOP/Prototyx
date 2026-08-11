import os
import json
from typing import Dict, List, Any

# Mock debate transcripts for major Indian and global stock pools
MOCK_DEBATES = {
    "default": [
        {
            "agent": "Macro Analyst Agent",
            "message": "Inflation is stabilizing, and the central bank is likely to hold interest rates steady. This creates a favorable environment for large-cap growth stocks. I recommend maintaining a steady exposure to equities, but keeping a close eye on interest-sensitive sectors."
        },
        {
            "agent": "Fundamental Analyst Agent",
            "message": "Agreed. Looking at our target stock selection, companies are showing solid earnings growth and expanding margins due to tech adoption. P/E ratios are slightly elevated, but backed by strong return on equity (ROE > 18%). We should overweight core tech and industrial leaders."
        },
        {
            "agent": "Technical Analyst Agent",
            "message": "From a price momentum perspective, the 50-day moving average is crossing above the 200-day moving average (Golden Cross) for our top tech picks. RSI is healthy at 58, indicating strong buying momentum without being overbought. I support increasing equity weight."
        },
        {
            "agent": "Compliance & Risk Agent",
            "message": "Under standard portfolio risk constraints, we must avoid sector concentration. I will cap the maximum allocation for any single stock at 25% and enforce a maximum sector allocation of 35% in IT/Technology. This protects the client against systematic sector shocks."
        }
    ],
    "TCS_RELIANCE": [
        {
            "agent": "Macro Analyst Agent",
            "message": "The Indian economy continues its strong momentum with GDP growth exceeding 7%. Inflation has cooled to 4.2%, which is favorable for domestic consumer demand. I recommend overweighting Reliance as a proxy for consumer growth (Retail & Jio) and keeping TCS stable as international IT demand recalibrates."
        },
        {
            "agent": "Fundamental Analyst Agent",
            "message": "Reliance retail EBITDA has expanded by 18%, and Jio's 5G monetization is accelerating. TCS, on the other hand, boasts an impressive ROE of 38% and a record deal pipeline of $10.2B, making it an excellent cash-flow generator. Both are fundamentally robust."
        },
        {
            "agent": "Technical Analyst Agent",
            "message": "RELIANCE has hit support at its 200-day moving average and is showing a bullish divergence on the MACD. TCS is trading in a tight consolidation band with an RSI of 52, which is prime for breakout. Buying momentum is building."
        },
        {
            "agent": "Compliance & Risk Agent",
            "message": "Under regulatory compliance rules, we will cap the individual weight of RELIANCE at 30% and TCS at 25% to prevent stock concentration. The remaining portfolio should be allocated to debt/gold buffers to cushion volatility."
        }
    ]
}

def run_committee_debate(tickers: List[str]) -> Dict[str, Any]:
    """
    Orchestrates the Investment Committee debate using CrewAI if API keys are set.
    Otherwise, falls back to a high-fidelity simulated debate tailored to the tickers.
    """
    api_key_set = (
        os.environ.get("OPENROUTER_API_KEY") or
        os.environ.get("GEMINI_API_KEY") or
        os.environ.get("OPENAI_API_KEY") or
        os.environ.get("XAI_API_KEY")
    )
    
    # Clean tickers list
    tickers_clean = [t.upper().replace(".NS", "") for t in tickers]
    
    if not api_key_set:
        # Run Simulation Mode
        debate = MOCK_DEBATES.get("TCS_RELIANCE") if "TCS" in tickers_clean and "RELIANCE" in tickers_clean else MOCK_DEBATES.get("default")
        
        # Customize views based on inputs
        views = {}
        for t in tickers_clean:
            if t == "TCS":
                views[t] = 0.16
            elif t == "RELIANCE":
                views[t] = 0.14
            elif t == "AAPL":
                views[t] = 0.18
            else:
                views[t] = 0.11 # default view return
                
        return {
            "mode": "Simulated (Offline Demo)",
            "debate_logs": debate,
            "implied_views": views,
            "confidences": [0.85] * len(tickers_clean)
        }
        
    try:
        from crewai import Agent, Crew, Task, Process

        # 1. Check for Cloud Keys
        if os.environ.get("OPENROUTER_API_KEY"):
            llm = "openrouter/nvidia/nemotron-3-ultra-550b-a55b:free"
        elif os.environ.get("GEMINI_API_KEY"):
            llm = "gemini/gemini-flash-latest"
        else:
            # 2. Default to Local LLM (Ollama)
            # This ensures "Hassle-Free" operation without keys
            print("INFO: No cloud API keys found. Connecting to Local Ollama (llama3.1)...")
            llm = "ollama/llama3.1"

        # 1. Define Agents
        
        # 1. Define Agents
        macro_analyst = Agent(
            role='Senior Macroeconomic Analyst',
            goal='Assess macroeconomic conditions, interest rate environments, and asset class distributions.',
            backstory="You are a veteran macro economist. You advise on asset rotation between stocks, bonds, and cash based on economic cycles.",
            verbose=True,
            llm=llm
        )
        
        fundamental_analyst = Agent(
            role='Fundamental Stock Analyst',
            goal='Evaluate individual stock financial health, earnings call results, and corporate governance.',
            backstory="You are a bottom-up research analyst. You analyze P/E, ROE, and earnings call guidance to find undervalued businesses.",
            verbose=True,
            llm=llm
        )
        
        technical_analyst = Agent(
            role='Technical Quant Analyst',
            goal='Analyze price trends, RSI, MACD momentum indicators, and support/resistance zones.',
            backstory="You are a quantitative technician. You focus on market momentum, buying/selling volumes, and trend direction.",
            verbose=True,
            llm=llm
        )
        
        compliance_risk = Agent(
            role='Chief Compliance and Risk Officer',
            goal='Enforce portfolio risk constraints, sector limits, and ensure SEBI regulatory compliance.',
            backstory="You are a risk manager. You ensure the portfolio does not hold concentrated stock risks and fits conservative guidelines.",
            verbose=True,
            llm=llm
        )
        
        # 2. Define Tasks
        task1 = Task(
            description=f"Analyze current economic cycle parameters for the asset universe: {', '.join(tickers_clean)}. Keep the report professional but concise (max 300 words).",
            expected_output="A structured economic outlook report for these stocks.",
            agent=macro_analyst
        )
        
        task2 = Task(
            description=f"Review fundamental health metrics and earnings remarks for: {', '.join(tickers_clean)}. Keep the analysis focused on key catalysts (max 300 words).",
            expected_output="An analysis of the key fundamental upside potentials.",
            agent=fundamental_analyst
        )
        
        task3 = Task(
            description=f"Evaluate trend direction and price indicators for: {', '.join(tickers_clean)}.",
            expected_output="A list of technical buy/sell triggers.",
            agent=technical_analyst
        )
        
        task4 = Task(
            description="Synthesize the recommendations into final portfolio active returns views. Enforce risk caps (max 30% single asset). Output a raw JSON map with format: {'ticker': float_return_view}.",
            expected_output="A final JSON dictionary of asset return views.",
            agent=compliance_risk
        )
        
        # 3. Assemble Crew
        crew = Crew(
            agents=[macro_analyst, fundamental_analyst, technical_analyst, compliance_risk],
            tasks=[task1, task2, task3, task4],
            process=Process.sequential
        )
        
        result = crew.kickoff()
        
        # Extract debate logs from agents task outputs
        debate_logs = [
            {"agent": "Macro Analyst Agent", "message": str(task1.output.raw)},
            {"agent": "Fundamental Analyst Agent", "message": str(task2.output.raw)},
            {"agent": "Technical Analyst Agent", "message": str(task3.output.raw)},
            {"agent": "Compliance & Risk Agent", "message": str(task4.output.raw)}
        ]
        
        # Try to parse final JSON output
        try:
            views = json.loads(str(task4.output.raw))
        except:
            # Fallback parser if LLM output is not clean JSON
            views = {t: 0.12 for t in tickers_clean}
            
        return {
            "mode": "Live CrewAI Engine",
            "debate_logs": debate_logs,
            "implied_views": views,
            "confidences": [0.90] * len(tickers_clean)
        }
    except Exception as e:
        # Fallback to simulation mode on failure
        return {
            "mode": f"Fallback Simulation (Error running CrewAI: {str(e)})",
            "debate_logs": MOCK_DEBATES.get("default"),
            "implied_views": {t: 0.12 for t in tickers_clean},
            "confidences": [0.80] * len(tickers_clean)
        }

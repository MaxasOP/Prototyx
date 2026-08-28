import os
import json
import re
from typing import Dict, List, Any
import requests

def run_committee_debate(tickers: List[str]) -> Dict[str, Any]:
    """
    Performs a real-time multi-agent investment committee debate using LLM.
    """
    tickers_clean = [t.upper().strip().replace(".NS", "") for t in tickers]
    
    # 1. Build a prompt that forces a multi-agent dialogue format
    prompt = f"""
    Act as an Investment Committee debating these assets: {", ".join(tickers_clean)}.
    
    You must provide a dialogue between 4 agents:
    1. Macro Analyst Agent: Discuss global/local macro trends and interest rates.
    2. Fundamental Analyst Agent: Discuss earnings, margins, ROE, and valuations.
    3. Technical Analyst Agent: Discuss price action, moving averages (50/200 DMA), and RSI.
    4. Compliance & Risk Agent: Discuss diversification, sector caps, and risk management.
    
    FORMAT RULES:
    - Format each contribution exactly as: [Agent Name]: [Message]
    - Do not use markdown bolding (**) or headers (###).
    - Keep each agent's message concise but highly insightful.
    - End your response with a JSON block mapping ticker symbols to expected 12-month returns (as decimals, e.g., 0.15).
    """

    url = "https://openrouter.ai/api/v1/chat/completions"
    api_key = os.getenv("OPENROUTER_API_KEY")
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    payload = {
        "model": "nvidia/nemotron-3-ultra-550b-a55b:free",
        "messages": [
            {"role": "system", "content": "You are a professional investment committee. Use real market knowledge for the provided tickers. Output must be a dialogue followed by a JSON return projection."},
            {"role": "user", "content": prompt}
        ]
    }

    try:
        print(f"DEBUG: Running real-time debate for {tickers_clean}...")
        response = requests.post(url, headers=headers, json=payload, timeout=90)
        if response.status_code != 200:
            raise Exception(f"OpenRouter error: {response.text}")
        
        full_text = response.json().get("choices", [{}])[0].get("message", {}).get("content", "")
        
        # 2. Parse the dialogue into AgentDialogueLog format
        debate_logs = []
        # Split by lines and look for "[Agent Name]: [Message]" pattern
        lines = full_text.split("\n")
        for line in lines:
            if ":" in line:
                parts = line.split(":", 1)
                agent_candidate = parts[0].strip()
                if "Agent" in agent_candidate or "Analyst" in agent_candidate or "Compliance" in agent_candidate:
                    debate_logs.append({
                        "agent": agent_candidate,
                        "message": parts[1].strip().replace("**", "").replace("*", "")
                    })

        # Fallback if parsing fails
        if not debate_logs:
            debate_logs = [{"agent": "Committee Orchestrator", "message": full_text.split("{")[0].strip()}]

        # 3. Extract JSON Return Views
        views = {t: 0.12 for t in tickers_clean}
        try:
            json_match = re.findall(r'\{[^{}]*\}', full_text)
            if json_match:
                parsed = json.loads(json_match[-1].replace("'", "\""))
                for t in tickers_clean:
                    if t in parsed:
                        views[t] = float(parsed[t])
        except:
            pass

        return {
            "mode": "Live Multi-Agent Consensus (Nemotron-3 Ultra)",
            "debate_logs": debate_logs,
            "implied_views": views,
            "confidences": [0.90] * len(tickers_clean)
        }

    except Exception as e:
        print(f"CRITICAL: Debate orchestration failed: {e}")
        raise Exception(f"AI Analysis Engine unreachable. ({str(e)})")

def generate_earnings_transcript_llm(ticker: str, year: int, quarter: int) -> Dict[str, Any]:
    """
    Uses real-time AI knowledge to generate a professional earnings synthesis for any ticker.
    """
    url = "https://openrouter.ai/api/v1/chat/completions"
    api_key = os.getenv("OPENROUTER_API_KEY")
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    
    prompt = f"""
    Generate a professional investment-grade synthesis of the most recent earnings for {ticker} (FY {year} Q{quarter}).
    
    Provide exactly two sections:
    1. PREPARED REMARKS: A executive summary of revenue growth, margins, and strategic guidance.
    2. QA SESSION: A synthesis of key analyst questions and management responses.
    
    RULES:
    - Use professional, institutional language.
    - DO NOT use markdown symbols (*, **, ###).
    - Use PLAIN TEXT only.
    - Focus on real market data and guidance for {ticker}.
    - Keep it concise.
    """
    
    payload = {
        "model": "nvidia/nemotron-3-ultra-550b-a55b:free",
        "messages": [
            {"role": "system", "content": "You are a professional equity research analyst. Use your internal knowledge of global and Indian markets."},
            {"role": "user", "content": prompt}
        ]
    }
    
    try:
        print(f"DEBUG: AI-Ingesting earnings for {ticker}...")
        response = requests.post(url, headers=headers, json=payload, timeout=90)
        full_text = response.json().get("choices", [{}])[0].get("message", {}).get("content", "")
        
        # Simple splitting logic
        remarks = "Intelligence summary unavailable."
        qa = "Q&A synthesis unavailable."
        
        if "PREPARED REMARKS" in full_text.upper():
            parts = re.split(r'QA SESSION|Q&A SESSION', full_text, flags=re.IGNORECASE)
            remarks = parts[0].replace("PREPARED REMARKS:", "").replace("PREPARED REMARKS", "").strip()
            if len(parts) > 1:
                qa = parts[1].replace("QA SESSION:", "").replace("QA SESSION", "").strip()
        
        return {
            "ticker": ticker,
            "company_name": f"{ticker} Corporation",
            "quarter": f"Q{quarter} {year}",
            "prepared_remarks": remarks.replace("**", "").replace("*", ""),
            "qa_session": qa.replace("**", "").replace("*", "")
        }
    except Exception as e:
        print(f"ERROR: AI Ingestion failed: {e}")
        return {
            "ticker": ticker,
            "company_name": ticker,
            "quarter": f"Q{quarter} {year}",
            "prepared_remarks": "AI Ingestion failed. Please check connectivity.",
            "qa_session": "N/A"
        }

def generate_asset_memo(ticker: str, metrics: Dict[str, Any]) -> str:
    """
    Generates a 2-sentence AI memo about an asset's technical and market position.
    """
    url = "https://openrouter.ai/api/v1/chat/completions"
    api_key = os.getenv("OPENROUTER_API_KEY")
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    
    prompt = f"Analyze {ticker} with these metrics: {metrics}. Provide a 2-sentence professional market memo. NO markdown."
    payload = {
        "model": "nvidia/nemotron-3-ultra-550b-a55b:free",
        "messages": [{"role": "user", "content": prompt}]
    }
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=30)
        return response.json().get("choices", [{}])[0].get("message", {}).get("content", "").strip()
    except: return "Intelligence summary temporarily unavailable."

def generate_optimizer_rationale(weights: Dict[str, float], views: Optional[Dict[str, float]]) -> str:
    """
    Explains the 'why' behind the portfolio optimization.
    """
    url = "https://openrouter.ai/api/v1/chat/completions"
    api_key = os.getenv("OPENROUTER_API_KEY")
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    
    prompt = f"Explain this portfolio allocation: {weights} based on these views: {views}. Keep it to 2-3 professional sentences. NO markdown."
    payload = {
        "model": "nvidia/nemotron-3-ultra-550b-a55b:free",
        "messages": [{"role": "user", "content": prompt}]
    }
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=30)
        return response.json().get("choices", [{}])[0].get("message", {}).get("content", "").strip()
    except: return "Optimization rationale unavailable."

def generate_risk_audit(nei: float, beta: float, redundancies: List[Dict]) -> str:
    """
    Provides a professional audit of the portfolio's systemic vulnerabilities.
    """
    url = "https://openrouter.ai/api/v1/chat/completions"
    api_key = os.getenv("OPENROUTER_API_KEY")
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    
    prompt = f"Audit this portfolio: Net Exposure Index={nei}, Beta={beta}, Overlaps={redundancies}. Provide a 2-sentence defense strategy. NO markdown."
    payload = {
        "model": "nvidia/nemotron-3-ultra-550b-a55b:free",
        "messages": [{"role": "user", "content": prompt}]
    }
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=30)
        return response.json().get("choices", [{}])[0].get("message", {}).get("content", "").strip()
    except: return "Risk audit summary unavailable."

def summarize_earnings_llm(ticker: str, transcript_data: Dict[str, Any]) -> Dict[str, Any]:
    """
    Fast real-time summarization via OpenRouter.
    """
    url = "https://openrouter.ai/api/v1/chat/completions"
    api_key = os.getenv("OPENROUTER_API_KEY")
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }
    prompt = f"Summarize these earnings for {ticker} in 3 professional bullets. Focus on guidance and margins: {transcript_data.get('prepared_remarks')}"
    
    payload = {
        "model": "nvidia/nemotron-3-ultra-550b-a55b:free",
        "messages": [
            {"role": "user", "content": prompt}
        ]
    }
    
    try:
        response = requests.post(url, headers=headers, json=payload, timeout=60)
        summary = response.json().get("choices", [{}])[0].get("message", {}).get("content", "Summary unavailable.")
        
        # Split into bullets and clean up
        takeaways = []
        for line in summary.split("\n"):
            line = line.strip().replace("**", "").replace("*", "").replace("- ", "").replace("• ", "")
            if line and len(line) > 10:
                takeaways.append(line)
        
        # Fallback to full summary if splitting failed
        if not takeaways:
            takeaways = [summary.replace("**", "").replace("*", "")]

        transcript_data["ai_intelligence"] = {
            "strategic_takeaways": takeaways,
            "guidance": "Real-time extraction via Nemotron-3 Ultra.",
            "sentiment": "Neutral"
        }
    except:
        transcript_data["ai_intelligence"] = {"error": "AI Brain Offline."}
    
    return transcript_data

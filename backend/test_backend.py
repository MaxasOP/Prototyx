import sys
import os

# Set Python path to include current backend directory
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from data.market_data import get_latest_metrics
from data.earnings import get_earnings_transcript
from quant.risk_mesh import calculate_exposure_mesh
from quant.optimizer import optimize_portfolio_mvo, optimize_portfolio_black_litterman
from agents.committee import run_committee_debate

def test_all():
    print("=== Testing Technical Indicators ===")
    tcs_metrics = get_latest_metrics("TCS")
    print(f"TCS Close: {tcs_metrics.get('close')}, RSI: {tcs_metrics.get('rsi')}")
    
    print("\n=== Testing Earnings Call Transcripts ===")
    tcs_transcript = get_earnings_transcript("TCS")
    print(f"Ticker: {tcs_transcript.get('ticker')}, Quarter: {tcs_transcript.get('quarter')}")
    print(f"Remarks Snippet: {tcs_transcript.get('prepared_remarks')[:150]}...")
    
    print("\n=== Testing Risk Exposure Mesh ===")
    tickers = ["TCS", "RELIANCE"]
    weights = [0.60, 0.40]
    mesh = calculate_exposure_mesh(tickers, weights)
    print(f"Net Exposure Index: {mesh.get('net_exposure_index')}")
    print(f"Correlation Matrix: {mesh.get('correlation_matrix')}")
    
    print("\n=== Testing Portfolio Optimizer ===")
    mvo_res = optimize_portfolio_mvo(tickers)
    print(f"MVO Weights: {mvo_res.get('weights')}, Expected Return: {mvo_res.get('expected_annual_return')}")
    
    print("\n=== Testing AI Agent Committee Debate ===")
    debate = run_committee_debate(tickers)
    print(f"Debate Mode: {debate.get('mode')}")
    print(f"Implied Views: {debate.get('implied_views')}")
    for log in debate.get('debate_logs')[:2]:
        print(f"[{log.get('agent')}]: {log.get('message')[:100]}...")
        
    print("\n=== Black-Litterman Optimization ===")
    bl_res = optimize_portfolio_black_litterman(tickers, debate.get('implied_views'))
    print(f"BL Weights: {bl_res.get('weights')}, Expected Return: {bl_res.get('expected_annual_return')}")
    print("\nAll Backend tests completed successfully!")

if __name__ == "__main__":
    test_all()

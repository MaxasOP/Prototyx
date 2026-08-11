import json
from quant.risk_mesh import calculate_exposure_mesh

def test():
    tickers = ["TCS", "RELIANCE", "AAPL", "INFY"]
    weights = [0.3, 0.4, 0.2, 0.1]

    print(f"Testing Risk Mesh for: {tickers}")
    try:
        result = calculate_exposure_mesh(tickers, weights)
        print("\n--- Result Summary ---")
        print(f"Tickers found: {result.get('tickers')}")
        print(f"Portfolio Beta: {result.get('portfolio_beta')}")

        # Test JSON serialization (where the crash happened)
        json_str = json.dumps(result)
        print("\nSUCCESS: Result is JSON compliant!")

    except Exception as e:
        print(f"\nFAILURE: {str(e)}")

if __name__ == "__main__":
    test()

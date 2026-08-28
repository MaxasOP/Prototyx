from typing import Dict, List, Any
import numpy as np

def calculate_exposure_mesh(tickers: List[str], weights: List[float]) -> Dict[str, Any]:
    """
    Computes real cross-asset risk mesh based on static sector correlations.
    No external APIs used.
    """
    # Clean tickers
    clean_tickers = [t.upper().strip().replace(".NS", "").replace(".BO", "") for t in tickers]
    
    # Static Correlation Logic (based on general market sectors)
    def get_static_corr(t1, t2):
        if t1 == t2: return 1.0
        # High correlation for same sectors (e.g., Indian IT)
        if t1 in ["TCS", "INFY"] and t2 in ["TCS", "INFY"]: return 0.85
        # Decent correlation for same market
        if t1 in ["TCS", "RELIANCE", "INFY"] and t2 in ["TCS", "RELIANCE", "INFY"]: return 0.45
        # Low correlation for cross-border (US vs India)
        if (t1 == "AAPL" or t2 == "AAPL"): return 0.15
        return 0.25

    corr_matrix = {}
    for t1 in clean_tickers:
        corr_matrix[t1] = {}
        for t2 in clean_tickers:
            corr_matrix[t1][t2] = get_static_corr(t1, t2)

    # Normalize weights
    norm_weights = np.array(weights) / sum(weights) if sum(weights) > 0 else np.zeros(len(weights))

    # Calculate Net Exposure Index (W^T * Corr * W)
    corr_array = np.array([[corr_matrix[t1][t2] for t2 in clean_tickers] for t1 in clean_tickers])
    nei = float(np.dot(norm_weights.T, np.dot(corr_array, norm_weights)))

    # Redundant Exposures
    redundant = []
    for i, t1 in enumerate(clean_tickers):
        for j, t2 in enumerate(clean_tickers):
            if i < j:
                val = corr_matrix[t1][t2]
                if val > 0.70:
                    redundant.append({
                        "ticker_1": t1, "ticker_2": t2, "correlation": val,
                        "warning": f"Structural Overlap: {t1} and {t2} correlation is high ({val})."
                    })

    # Portfolio Beta (Static Estimates)
    betas = {t: (1.2 if t == "AAPL" else 0.95) for t in clean_tickers}
    portfolio_beta = float(np.dot(norm_weights, [betas.get(t, 1.0) for t in clean_tickers]))

    from agents.committee import generate_risk_audit
    risk_audit = generate_risk_audit(round(nei, 3), round(portfolio_beta, 2), redundant)

    return {
        "tickers": clean_tickers,
        "weights": [round(float(w), 3) for w in norm_weights],
        "correlation_matrix": corr_matrix,
        "redundant_exposures": redundant,
        "hedged_positions": [],
        "net_exposure_index": round(nei, 3),
        "betas": betas,
        "portfolio_beta": round(portfolio_beta, 2),
        "ai_risk_audit": risk_audit,
        "source": "Local Structural Analysis"
    }

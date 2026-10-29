package com.example.prototyx.ui.main

import com.example.prototyx.data.model.OptimizeResponse
import com.example.prototyx.data.model.RiskMeshResponse
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class MainScreenViewModelTest {
    @Test fun optimizerContractPreservesDecimalWeights() {
        val result = Gson().fromJson("""{"weights":{"AAPL":0.4,"MSFT":0.6},"expected_annual_return":0.12,"annual_volatility":0.2,"sharpe_ratio":0.6,"method":"Historical minimum variance"}""", OptimizeResponse::class.java)
        assertEquals(1.0, result.weights.values.sum(), 0.00001)
        assertEquals(0.12, result.expectedAnnualReturn, 0.00001)
    }
    @Test fun riskContractPreservesCorrelationAndBeta() {
        val result = Gson().fromJson("""{"tickers":["AAPL"],"weights":[1.0],"correlation_matrix":{"AAPL":{"AAPL":1.0}},"redundant_exposures":[],"hedged_positions":[],"net_exposure_index":1.0,"betas":{"AAPL":1.2},"portfolio_beta":1.2}""", RiskMeshResponse::class.java)
        assertEquals(1.2, result.portfolioBeta, 0.00001)
        assertEquals(1.0, result.correlationMatrix["AAPL"]!!["AAPL"]!!, 0.00001)
    }
}

package com.example.prototyx.data

import com.example.prototyx.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MockDataRepository : DataRepository {
    override val holdings: Flow<Map<String, Float>> = flow { emit(mapOf("TCS" to 0.30f, "RELIANCE" to 0.40f, "AAPL" to 0.20f, "INFY" to 0.10f)) }

    override suspend fun login(request: LoginRequest): AuthResponse {
        return AuthResponse("mock_token", "bearer", UserProfile(request.email, "Mock User"))
    }

    override suspend fun register(request: RegisterRequest): AuthResponse {
        return AuthResponse("mock_token", "bearer", UserProfile(request.email, request.name))
    }

    override suspend fun syncWithCloud() {
        // Mock sync
    }

    override suspend fun logout() {
        // Mock logout
    }

    override suspend fun updateHolding(ticker: String, weight: Float) {
        // Mock implementation
    }

    override suspend fun removeHolding(ticker: String) {
        // Mock implementation
    }
    
    override suspend fun searchTickers(query: String?, sector: String?): List<TickerSearchResponse> = listOf(
        TickerSearchResponse("TCS", "Tata Consultancy Services", "Technology", "IT Services", "NSE")
    )
    
    override suspend fun getIndicators(ticker: String): MarketIndicatorsResponse = MarketIndicatorsResponse(
        ticker = ticker,
        close = 1000.0,
        sma50 = 950.0,
        sma200 = 900.0,
        rsi = 60.0,
        macd = 5.0,
        macdSignal = 4.0,
        macdHist = 1.0,
        bbUpper = 1050.0,
        bbMiddle = 1000.0,
        bbLower = 950.0,
        peRatio = 30.0,
        marketCap = 10000000.0,
        fiftyTwoWeekHigh = 1100.0,
        fiftyTwoWeekLow = 800.0,
        dividendYield = 0.02,
        aiMemo = "TCS is currently exhibiting a strong bullish divergence on the daily RSI while maintaining a resilient 24.5% operating margin despite sector-wide headwinds. The formation of a classic base at the 200-DMA suggests a favorable accumulation zone for long-term investors."
    )
    
    override suspend fun getTranscript(ticker: String, year: Int, quarter: Int): EarningsTranscriptResponse = EarningsTranscriptResponse(
        ticker = ticker,
        companyName = if (ticker == "TCS") "Tata Consultancy Services" else "Reliance Industries",
        quarter = "Q1 2027",
        preparedRemarks = "We have started the year on a strong note, with revenue growing 3.9% year-on-year in constant currency. Growth was driven by strong demand in our Cloud transformation and Generative AI segments. Operating margin was resilient at 24.7%. Our order book remains healthy at $10.2 billion. We are seeing a shift in client spending toward high-ROI automation projects as enterprises recalibrate for the AI-first era.",
        qaSession = "Analyst: Can you speak to the margin outlook given wage hikes?\nManagement: We have managed the impact through improved utilization and operational efficiency. We expect margins to trend toward the 25-26% band as AI-led automation scales.",
        aiIntelligence = AiIntelligence(
            strategicTakeaways = listOf(
                "Strong 3.9% CC revenue growth led by Cloud and GenAI.",
                "Order book at record $10.2B indicates high demand durability.",
                "Margin expansion target of 25-26% through automation efficiency."
            ),
            guidance = "Real-time extraction via Nemotron-3 Ultra.",
            sentiment = "Positive"
        )
    )
    
    override suspend fun getRiskMesh(tickers: List<String>, weights: List<Double>): RiskMeshResponse = RiskMeshResponse(
        tickers = tickers,
        weights = weights,
        correlationMatrix = tickers.associateWith { row -> tickers.associateWith { col -> if (row == col) 1.0 else (if (row == "TCS" && col == "INFY") 0.85 else 0.25) } },
        redundantExposures = listOf(RedundantExposure("TCS", "INFY", 0.85, "High concentration risk: TCS and INFY exhibit extreme correlation, reducing portfolio resilience to IT sector shocks.")),
        hedgedPositions = listOf(HedgedPosition("RELIANCE", "NIFTY-PE", -0.4, "Natural hedge detected via derivative buffer.")),
        netExposureIndex = 0.42,
        betas = tickers.associateWith { if (it == "AAPL") 1.2 else 0.95 },
        portfolioBeta = 1.05,
        aiRiskAudit = "The portfolio is currently well-diversified but shows a structural overlap in the Indian IT sector via TCS and INFY. To enhance resilience, I recommend a 5% shift from IT into cash or US-indexed assets to lower the Net Exposure Index below 0.40."
    )
    
    override suspend fun getOptimization(tickers: List<String>, views: Map<String, Double>?): OptimizeResponse = OptimizeResponse(
        weights = tickers.zip(listOf(0.35, 0.30, 0.20, 0.15)).toMap(),
        expectedAnnualReturn = 0.154,
        annualVolatility = 0.112,
        sharpeRatio = 1.28,
        method = "Black-Litterman Strategic Optimization",
        aiRationale = "The optimization engine has slightly tilted the portfolio towards Apple to capitalize on your high conviction view while maintaining a core defensive weight in TCS to buffer against potential volatility. This results in a superior Sharpe ratio of 1.28 by minimizing technical overlap."
    )
    
    override suspend fun runDebate(tickers: List<String>): DebateResponse = DebateResponse(
        mode = "AI Investment Committee (Grok-Beta Engine)",
        debateLogs = listOf(
            AgentDebateLog("Macro Analyst Agent", "Inflation is stabilizing, and the central bank is likely to hold interest rates steady. This creates a favorable environment for large-cap growth stocks. I recommend maintaining a steady exposure to equities, but keeping a close eye on interest-sensitive sectors."),
            AgentDebateLog("Fundamental Analyst Agent", "Agreed. Looking at our target stock selection, companies are showing solid earnings growth and expanding margins due to tech adoption. P/E ratios are slightly elevated, but backed by strong return on equity (ROE > 18%). We should overweight core tech and industrial leaders."),
            AgentDebateLog("Technical Analyst Agent", "From a price momentum perspective, the 50-day moving average is crossing above the 200-day moving average (Golden Cross) for our top tech picks. RSI is healthy at 58, indicating strong buying momentum without being overbought. I support increasing equity weight."),
            AgentDebateLog("Compliance & Risk Agent", "Under standard portfolio risk constraints, we must avoid sector concentration. I will cap the maximum allocation for any single stock at 25% and enforce a maximum sector allocation of 35% in IT/Technology. This protects the client against systematic sector shocks.")
        ),
        impliedViews = tickers.associateWith { if (it == "TCS") 0.16 else if (it == "RELIANCE") 0.14 else 0.12 },
        confidences = tickers.map { 0.85 }
    )

    override suspend fun runConsult(query: String): ConsultResponse = ConsultResponse(
        asset = "TCS",
        recommendation = "STRATEGIC ACCUMULATE: TCS is currently trading at a 12% discount to its 5-year historical average P/E. The latest earnings transcript reveals a massive pivot towards high-margin AI infrastructure deals, which are not yet fully priced in by the market. Technical indicators show a classic 'Cup and Handle' breakout pattern forming on the weekly chart, supported by rising accumulation-distribution scores. Risk-adjusted returns for the upcoming quarter look exceptionally favorable compared to broader NIFTY-IT peers.",
        dataSummary = mapOf(
            "current_price" to 4250.75,
            "analyst_consensus" to "Strong Buy",
            "upside_potential" to "14.2%",
            "risk_score" to 3,
            "ai_conviction" to 0.89
        )
    )
}

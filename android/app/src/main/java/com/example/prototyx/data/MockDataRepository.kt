package com.example.prototyx.data

import com.example.prototyx.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MockDataRepository : DataRepository {
    override val data: Flow<List<String>> = flow { emit(listOf("TCS", "RELIANCE", "AAPL", "INFY")) }
    
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
        dividendYield = 0.02
    )
    
    override suspend fun getTranscript(ticker: String, year: Int, quarter: Int): EarningsTranscriptResponse = EarningsTranscriptResponse(
        ticker = ticker,
        companyName = if (ticker == "TCS") "Tata Consultancy Services" else "Reliance Industries",
        quarter = "Q3 2026",
        preparedRemarks = "We are seeing strong demand in Cloud transformation and GenAI workloads. Operating margins have remained resilient at 24.5%. Our order book stands at a record $10.2B this quarter, driven by large deals in the BFSI and Retail sectors. We remain optimistic about long-term growth despite short-term macro headwinds in European markets.",
        qaSession = "Analyst: What is the outlook for deal ramp-ups in Q4?\nManagement: We expect conversion to accelerate as client budgets for 2027 are finalized. BFSI is showing early signs of recovery.\nAnalyst: Any impact from pricing pressure?\nManagement: Our value-led approach has helped us maintain premium pricing in specialized AI and Cybersecurity service lines."
    )
    
    override suspend fun getRiskMesh(tickers: List<String>, weights: List<Double>): RiskMeshResponse = RiskMeshResponse(
        tickers = tickers,
        weights = weights,
        correlationMatrix = tickers.associateWith { row -> tickers.associateWith { col -> if (row == col) 1.0 else (if (row == "TCS" && col == "INFY") 0.85 else 0.25) } },
        redundantExposures = listOf(RedundantExposure("TCS", "INFY", 0.85, "High concentration risk: TCS and INFY exhibit extreme correlation, reducing portfolio resilience to IT sector shocks.")),
        hedgedPositions = listOf(HedgedPosition("RELIANCE", "NIFTY-PE", -0.4, "Natural hedge detected via derivative buffer.")),
        netExposureIndex = 0.42,
        betas = tickers.associateWith { if (it == "AAPL") 1.2 else 0.95 },
        portfolioBeta = 1.05
    )
    
    override suspend fun getOptimization(tickers: List<String>, views: Map<String, Double>?): OptimizeResponse = OptimizeResponse(
        weights = tickers.zip(listOf(0.35, 0.30, 0.20, 0.15)).toMap(),
        expectedAnnualReturn = 0.154,
        annualVolatility = 0.112,
        sharpeRatio = 1.28,
        method = "Black-Litterman Strategic Optimization"
    )
    
    override suspend fun runDebate(tickers: List<String>): DebateResponse = DebateResponse(
        mode = "AI Investment Committee (Grok-Beta Engine)",
        debateLogs = listOf(
            AgentDebateLog("Macro Analyst Agent", "Global liquidity remains tight, but the domestic consumption story in India is decoupled. We should prioritize energy and consumer conglomerates over export-oriented tech in the short term."),
            AgentDebateLog("Fundamental Analyst Agent", "TCS's cash flow yield of 4.8% provides a significant margin of safety. While INFY has slightly higher growth, TCS's balance sheet management is superior for a conservative portfolio."),
            AgentDebateLog("Technical Quant Analyst Agent", "RELIANCE has found strong support at its 200-DMA with bullish MACD crossover. AAPL is showing relative strength against the NASDAQ index. Recommend overweight positions."),
            AgentDebateLog("Compliance & Risk Agent", "To maintain SEBI-tier compliance, we will enforce a 25% individual stock cap. I am flagging the IT sector concentration for re-balancing to ensure net exposure stays below 0.5.")
        ),
        impliedViews = tickers.associateWith { if (it == "TCS") 0.16 else if (it == "RELIANCE") 0.14 else 0.12 },
        confidences = tickers.map { 0.85 }
    )

    override suspend fun getPaytmLoginUrl(): PaytmLoginUrlResponse = PaytmLoginUrlResponse(
        url = "http://10.0.2.2:8000/api/paytm/callback?request_token=mock_request_token_12345",
        mode = "Mock Presentation Mode"
    )

    override suspend fun getPaytmHoldings(): PaytmHoldingsResponse = PaytmHoldingsResponse(
        connected = true,
        holdings = listOf(
            PaytmAsset("TCS", "Tata Consultancy Services Ltd", 10, 35000.0, 0.35),
            PaytmAsset("RELIANCE", "Reliance Industries Ltd", 15, 36000.0, 0.36),
            PaytmAsset("AAPL", "Apple Inc.", 12, 19200.0, 0.19),
            PaytmAsset("INFY", "Infosys Ltd", 8, 10000.0, 0.10)
        ),
        source = "Simulated Mock Database"
    )

    override suspend fun disconnectPaytm(): PaytmDisconnectResponse = PaytmDisconnectResponse(
        status = "success",
        message = "Disconnected from mock session"
    )
}

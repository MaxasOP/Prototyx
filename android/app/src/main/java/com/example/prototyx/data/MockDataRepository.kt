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
        companyName = "Sample Company",
        quarter = "Q$quarter $year",
        preparedRemarks = "Sample prepared remarks for preview.",
        qaSession = "Sample Q&A session."
    )
    
    override suspend fun getRiskMesh(tickers: List<String>, weights: List<Double>): RiskMeshResponse = RiskMeshResponse(
        tickers = tickers,
        weights = weights,
        correlationMatrix = tickers.associateWith { row -> tickers.associateWith { col -> if (row == col) 1.0 else 0.5 } },
        redundantExposures = listOf(RedundantExposure("TCS", "INFY", 0.95, "High correlation detected.")),
        hedgedPositions = listOf(HedgedPosition("RELIANCE", "NIFTY", -0.8, "Partial hedge.")),
        netExposureIndex = 0.45,
        betas = tickers.associateWith { 1.1 },
        portfolioBeta = 1.1
    )
    
    override suspend fun getOptimization(tickers: List<String>, views: Map<String, Double>?): OptimizeResponse = OptimizeResponse(
        weights = tickers.associateWith { 1.0 / tickers.size },
        expectedAnnualReturn = 0.12,
        annualVolatility = 0.15,
        sharpeRatio = 0.8,
        method = "Mean-Variance"
    )
    
    override suspend fun runDebate(tickers: List<String>): DebateResponse = DebateResponse(
        mode = "Debate",
        debateLogs = listOf(AgentDebateLog("BullAgent", "The portfolio looks strong.")),
        impliedViews = tickers.associateWith { 0.05 },
        confidences = tickers.map { 0.8 }
    )
}

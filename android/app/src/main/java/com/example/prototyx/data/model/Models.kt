package com.example.prototyx.data.model

import com.google.gson.annotations.SerializedName

// --- Request Bodies ---
data class TickerListRequest(
    val tickers: List<String>
)

data class RiskMeshRequest(
    val tickers: List<String>,
    val weights: List<Double>
)

data class OptimizeRequest(
    val tickers: List<String>,
    val views: Map<String, Double>? = null
)

data class ConsultRequest(
    val query: String
)

// --- Auth Models ---
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String? = null
)

data class AuthResponse(
    val accessToken: String,
    val userEmail: String
)

data class UserProfile(
    val email: String,
    val name: String?
)

// --- Sync Models ---
data class SyncHoldingsRequest(
    val holdings: Map<String, Float>
)

// --- Response Bodies ---
data class TickerSearchResponse(
    val ticker: String,
    val name: String,
    val sector: String,
    val industry: String,
    val exchange: String
)

data class MarketIndicatorsResponse(
    val ticker: String,
    val close: Double,
    @SerializedName("sma_50") val sma50: Double? = null,
    @SerializedName("sma_200") val sma200: Double? = null,
    val rsi: Double? = null,
    val macd: Double? = null,
    @SerializedName("macd_signal") val macdSignal: Double? = null,
    @SerializedName("macd_hist") val macdHist: Double? = null,
    @SerializedName("bb_upper") val bbUpper: Double? = null,
    @SerializedName("bb_middle") val bbMiddle: Double? = null,
    @SerializedName("bb_lower") val bbLower: Double? = null,
    @SerializedName("pe_ratio") val peRatio: Double? = null,
    @SerializedName("market_cap") val marketCap: Double? = null,
    @SerializedName("52_week_high") val fiftyTwoWeekHigh: Double? = null,
    @SerializedName("52_week_low") val fiftyTwoWeekLow: Double? = null,
    @SerializedName("dividend_yield") val dividendYield: Double? = null,
    @SerializedName("ai_memo") val aiMemo: String? = null
)

data class AiIntelligence(
    @SerializedName("strategic_takeaways") val strategicTakeaways: List<String>?,
    val guidance: String?,
    val sentiment: String?
)

data class EarningsTranscriptResponse(
    val ticker: String,
    @SerializedName("company_name") val companyName: String,
    val quarter: String,
    @SerializedName("prepared_remarks") val preparedRemarks: String,
    @SerializedName("qa_session") val qaSession: String,
    @SerializedName("ai_intelligence") val aiIntelligence: AiIntelligence? = null
)

data class RedundantExposure(
    @SerializedName("ticker_1") val ticker1: String,
    @SerializedName("ticker_2") val ticker2: String,
    val correlation: Double,
    val warning: String
)

data class HedgedPosition(
    @SerializedName("ticker_1") val ticker1: String,
    @SerializedName("ticker_2") val ticker2: String,
    val correlation: Double,
    val details: String
)

data class RiskMeshResponse(
    val tickers: List<String>,
    val weights: List<Double>,
    @SerializedName("correlation_matrix") val correlationMatrix: Map<String, Map<String, Double>>,
    @SerializedName("redundant_exposures") val redundantExposures: List<RedundantExposure>,
    @SerializedName("hedged_positions") val hedgedPositions: List<HedgedPosition>,
    @SerializedName("net_exposure_index") val netExposureIndex: Double,
    val betas: Map<String, Double>,
    @SerializedName("portfolio_beta") val portfolioBeta: Double,
    @SerializedName("ai_risk_audit") val aiRiskAudit: String? = null
)

data class OptimizeResponse(
    val weights: Map<String, Double>,
    @SerializedName("expected_annual_return") val expectedAnnualReturn: Double,
    @SerializedName("annual_volatility") val annualVolatility: Double,
    @SerializedName("sharpe_ratio") val sharpeRatio: Double,
    val method: String,
    @SerializedName("ai_rationale") val aiRationale: String? = null
)

data class AgentDebateLog(
    val agent: String,
    val message: String
)

data class DebateResponse(
    val mode: String?,
    @SerializedName("debate_logs") val debateLogs: List<AgentDebateLog>?,
    @SerializedName("implied_views") val impliedViews: Map<String, Double>?,
    val confidences: List<Double>?
)

data class ConsultResponse(
    val asset: String?,
    val recommendation: String?,
    @SerializedName("data_summary") val dataSummary: Map<String, Any>?
)

// --- Dual UX & FINNY Autonomous Agent Models ---
enum class UserExperienceMode {
    SIMPLE, PRO
}

data class PricePoint(
    val timestamp: String,
    val close: Float,
    val volume: Float = 0f,
    val sma50: Float? = null,
    val sma200: Float? = null,
    val rsi: Float? = null
)

data class CandleStickData(
    val timestamp: String,
    val open: Float,
    val high: Float,
    val low: Float,
    val close: Float,
    val volume: Float
)

data class StockChartResponse(
    val ticker: String,
    val currentPrice: Float,
    val priceChange: Float,
    val priceChangePercent: Float,
    val pricePoints: List<PricePoint>,
    val candleSticks: List<CandleStickData> = emptyList()
)

data class NewsSentimentItem(
    val id: String,
    val title: String,
    val source: String,
    val timestamp: String,
    val tickers: List<String>,
    val sentiment: String, // "BULLISH", "NEUTRAL", "BEARISH"
    val sentimentScore: Float, // -1.0 to 1.0
    val aiImpactAnalysis: String,
    val plainEnglishSummary: String
)

data class ScreenerResultItem(
    val ticker: String,
    val name: String,
    val sector: String,
    val price: Float,
    val changePercent: Float,
    val peRatio: Float,
    val dividendYield: Float,
    val rsi: Float,
    val riskLevel: String, // "SAFE", "BALANCED", "GROWTH"
    val aiAnalysis: String,
    val plainEnglishSummary: String
)

data class StressTestRequest(
    val tickers: List<String>,
    val weights: List<Double>,
    val scenarioId: String
)

data class StressTestResult(
    val scenarioName: String,
    val portfolioReturnChange: Double,
    val initialVaR: Double,
    val stressedVaR: Double,
    val initialDrawdown: Double,
    val stressedDrawdown: Double,
    val aiHedgingStrategy: String,
    val plainEnglishVerdict: String
)

data class FinnyStrategyReport(
    val portfolioHealthScore: Int, // 0 to 100
    val riskStatus: String, // "SAFE", "BALANCED", "ACTION_RECOMMENDED"
    val keyInsights: List<String>,
    val suggestedRebalance: Map<String, Double>,
    val selfLearnedMarketRegime: String,
    val dailyBriefingSummary: String
)

data class FinnyPortfolioAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: String, // "INFO", "WARNING", "URGENT"
    val timestamp: String,
    val recommendedAction: String,
    val plainEnglishAction: String
)


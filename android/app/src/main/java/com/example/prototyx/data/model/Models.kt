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
    @SerializedName("sma_50") val sma50: Double?,
    @SerializedName("sma_200") val sma200: Double?,
    val rsi: Double?,
    val macd: Double?,
    @SerializedName("macd_signal") val macdSignal: Double?,
    @SerializedName("macd_hist") val macdHist: Double?,
    @SerializedName("bb_upper") val bbUpper: Double?,
    @SerializedName("bb_middle") val bbMiddle: Double?,
    @SerializedName("bb_lower") val bbLower: Double?,
    @SerializedName("pe_ratio") val peRatio: Double?,
    @SerializedName("market_cap") val marketCap: Double?,
    @SerializedName("52_week_high") val fiftyTwoWeekHigh: Double?,
    @SerializedName("52_week_low") val fiftyTwoWeekLow: Double?,
    @SerializedName("dividend_yield") val dividendYield: Double?,
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

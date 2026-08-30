package com.example.prototyx.data

import com.example.prototyx.data.local.dao.PrototyxDao
import com.example.prototyx.data.local.entity.Holding
import com.example.prototyx.data.local.entity.MarketIndicatorCache
import com.example.prototyx.data.model.*
import com.example.prototyx.data.network.RetrofitInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface DataRepository {
    val holdings: Flow<Map<String, Float>>
    
    suspend fun login(request: LoginRequest): AuthResponse
    suspend fun register(request: RegisterRequest): AuthResponse
    suspend fun syncWithCloud()
    suspend fun logout()

    suspend fun updateHolding(ticker: String, weight: Float)
    suspend fun removeHolding(ticker: String)
    suspend fun searchTickers(query: String? = null, sector: String? = null): List<TickerSearchResponse>
    suspend fun getIndicators(ticker: String): MarketIndicatorsResponse
    suspend fun getTranscript(ticker: String, year: Int = 2026, quarter: Int = 3): EarningsTranscriptResponse
    suspend fun getRiskMesh(tickers: List<String>, weights: List<Double>): RiskMeshResponse
    suspend fun getOptimization(tickers: List<String>, views: Map<String, Double>? = null): OptimizeResponse
    suspend fun runDebate(tickers: List<String>): DebateResponse
    suspend fun runConsult(query: String): ConsultResponse
}

class DefaultDataRepository(private val dao: PrototyxDao) : DataRepository {
    
    override val holdings: Flow<Map<String, Float>> = dao.getHoldingsFlow().map { list ->
        list.associate { it.ticker to it.weight }
    }

    override suspend fun login(request: LoginRequest): AuthResponse {
        return RetrofitInstance.apiService.login(request)
    }

    override suspend fun register(request: RegisterRequest): AuthResponse {
        return RetrofitInstance.apiService.register(request)
    }

    override suspend fun logout() {
        dao.clearHoldings()
        dao.clearIndicatorCache()
    }

    override suspend fun syncWithCloud() {
        val localHoldings = dao.getAllHoldings().associate { it.ticker to it.weight }
        val cloudHoldings = RetrofitInstance.apiService.syncHoldings(SyncHoldingsRequest(localHoldings))
        
        // Update local with cloud truth
        val newHoldings = cloudHoldings.map { Holding(it.key, it.value) }
        dao.insertHoldings(newHoldings)
    }

    override suspend fun updateHolding(ticker: String, weight: Float) {
        dao.insertHoldings(listOf(Holding(ticker, weight)))
        try {
            syncWithCloud()
        } catch (e: Exception) {
            // Silently fail sync, will retry on manual sync
        }
    }

    override suspend fun removeHolding(ticker: String) {
        dao.deleteHolding(ticker)
        try {
            syncWithCloud()
        } catch (e: Exception) {
            // Silently fail sync
        }
    }

    override suspend fun searchTickers(query: String?, sector: String?): List<TickerSearchResponse> {
        return RetrofitInstance.apiService.searchTickers(query, sector)
    }

    override suspend fun getIndicators(ticker: String): MarketIndicatorsResponse {
        // 1. Try to get from cache first
        val cached = dao.getIndicator(ticker)
        if (cached != null && (System.currentTimeMillis() - cached.lastUpdated) < 30 * 60 * 1000) { // 30 min cache
            return MarketIndicatorsResponse(
                ticker = cached.ticker,
                close = cached.close,
                sma50 = cached.sma50,
                sma200 = cached.sma200,
                rsi = cached.rsi,
                macd = null, // Not cached for simplicity in this example
                macdSignal = null,
                macdHist = null,
                bbUpper = null,
                bbMiddle = null,
                bbLower = null,
                peRatio = cached.peRatio,
                marketCap = null,
                fiftyTwoWeekHigh = null,
                fiftyTwoWeekLow = null,
                dividendYield = cached.dividendYield,
                aiMemo = cached.aiMemo
            )
        }

        // 2. Fetch from network
        val networkData = RetrofitInstance.apiService.getIndicators(ticker)

        // 3. Update cache
        dao.insertIndicator(
            MarketIndicatorCache(
                ticker = networkData.ticker,
                close = networkData.close,
                sma50 = networkData.sma50,
                sma200 = networkData.sma200,
                rsi = networkData.rsi,
                peRatio = networkData.peRatio,
                dividendYield = networkData.dividendYield,
                aiMemo = networkData.aiMemo
            )
        )

        return networkData
    }

    override suspend fun getTranscript(ticker: String, year: Int, quarter: Int): EarningsTranscriptResponse {
        return RetrofitInstance.apiService.getTranscript(ticker, year, quarter)
    }

    override suspend fun getRiskMesh(tickers: List<String>, weights: List<Double>): RiskMeshResponse {
        return RetrofitInstance.apiService.getRiskMesh(RiskMeshRequest(tickers, weights))
    }

    override suspend fun getOptimization(tickers: List<String>, views: Map<String, Double>?): OptimizeResponse {
        return RetrofitInstance.apiService.getOptimization(OptimizeRequest(tickers, views))
    }

    override suspend fun runDebate(tickers: List<String>): DebateResponse {
        return RetrofitInstance.apiService.runDebate(TickerListRequest(tickers))
    }

    override suspend fun runConsult(query: String): ConsultResponse {
        return RetrofitInstance.apiService.runConsult(ConsultRequest(query))
    }
}

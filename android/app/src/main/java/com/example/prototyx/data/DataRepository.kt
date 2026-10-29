package com.example.prototyx.data

import com.example.prototyx.data.local.dao.PrototyxDao
import com.example.prototyx.data.local.entity.Holding
import com.example.prototyx.data.local.entity.MarketIndicatorCache
import com.example.prototyx.data.model.*
import com.example.prototyx.data.network.RetrofitInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface DataRepository {
    val holdings: Flow<Map<String, Float>>
    
    suspend fun login(request: LoginRequest): AuthResponse
    suspend fun register(request: RegisterRequest): AuthResponse
    suspend fun syncWithCloud()
    suspend fun logout()
    suspend fun restoreHoldings() {}

    suspend fun updateHolding(ticker: String, weight: Float)
    suspend fun removeHolding(ticker: String)
    suspend fun replaceHoldings(holdings: Map<String, Float>)
    suspend fun searchTickers(query: String? = null, sector: String? = null): List<TickerSearchResponse>
    suspend fun getIndicators(ticker: String): MarketIndicatorsResponse
    suspend fun getTranscript(ticker: String, year: Int = 2026, quarter: Int = 3): EarningsTranscriptResponse
    suspend fun getRiskMesh(tickers: List<String>, weights: List<Double>): RiskMeshResponse
    suspend fun getOptimization(tickers: List<String>, views: Map<String, Double>? = null): OptimizeResponse
    suspend fun runDebate(tickers: List<String>): DebateResponse
    suspend fun runConsult(query: String): ConsultResponse
    suspend fun getStockChart(ticker: String, period: String = "1M"): StockChartResponse
    suspend fun getNewsSentiment(ticker: String? = null): List<NewsSentimentItem>
    suspend fun searchScreener(query: String? = null, riskLevel: String? = null): List<ScreenerResultItem>
    suspend fun runStressTest(tickers: List<String>, weights: List<Double>, scenarioId: String): StressTestResult
    suspend fun getFinnyReport(): FinnyStrategyReport
}

class DefaultDataRepository(private val dao: PrototyxDao) : DataRepository {
    private val holdingsMutex = Mutex()
    override suspend fun restoreHoldings() = holdingsMutex.withLock {
        val cloud = RetrofitInstance.apiService.getHoldings()
        dao.replaceHoldings(cloud.map { Holding(it.key, it.value) })
    }
    
    override val holdings: Flow<Map<String, Float>> = dao.getHoldingsFlow().map { list ->
        list.associate { it.ticker to it.weight }
    }

    override suspend fun login(request: LoginRequest): AuthResponse {
        val response = RetrofitInstance.apiService.login(request)
        dao.clearHoldings()
        dao.clearIndicatorCache()
        return response
    }

    override suspend fun register(request: RegisterRequest): AuthResponse {
        val response = RetrofitInstance.apiService.register(request)
        dao.clearHoldings()
        dao.clearIndicatorCache()
        return response
    }

    override suspend fun logout() {
        dao.clearHoldings()
        dao.clearIndicatorCache()
    }

    override suspend fun syncWithCloud() = holdingsMutex.withLock {
        syncLocked()
    }

    private suspend fun syncLocked() {
        val localHoldings = dao.getAllHoldings().associate { it.ticker to it.weight }
        val cloudHoldings = RetrofitInstance.apiService.syncHoldings(SyncHoldingsRequest(localHoldings))
        
        // Update local with cloud truth
        val newHoldings = cloudHoldings.map { Holding(it.key, it.value) }
        dao.replaceHoldings(newHoldings)
    }

    override suspend fun updateHolding(ticker: String, weight: Float) = holdingsMutex.withLock {
        require(ticker.isNotBlank() && weight.isFinite() && weight > 0f && weight <= 1f) { "Enter a ticker and a weight greater than 0 and at most 1." }
        val current = dao.getAllHoldings()
        require(current.filter { it.ticker != ticker }.sumOf { it.weight.toDouble() } + weight <= 1.00001) { "Total allocation cannot exceed 100%. Reduce another holding first." }
        dao.insertHoldings(listOf(Holding(ticker, weight)))
        syncLocked()
    }

    override suspend fun removeHolding(ticker: String) = holdingsMutex.withLock {
        dao.deleteHolding(ticker)
        syncLocked()
    }

    override suspend fun replaceHoldings(holdings: Map<String, Float>) = holdingsMutex.withLock {
        require(holdings.all { (ticker, weight) -> ticker.isNotBlank() && weight.isFinite() && weight >= 0f && weight <= 1f })
        require(holdings.values.sum() <= 1.00001f) { "Total allocation cannot exceed 100%." }
        dao.replaceHoldings(holdings.filterValues { it > 0f }.map { Holding(it.key, it.value) })
        syncLocked()
    }

    override suspend fun searchTickers(query: String?, sector: String?): List<TickerSearchResponse> {
        return RetrofitInstance.apiService.searchTickers(query, sector)
    }

    override suspend fun getIndicators(ticker: String): MarketIndicatorsResponse {
        // 1. Try to get from cache first if fresh
        val cached = dao.getIndicator(ticker)
        if (cached != null && (System.currentTimeMillis() - cached.lastUpdated) < 30 * 60 * 1000) { // 30 min cache
            return MarketIndicatorsResponse(
                ticker = cached.ticker,
                close = cached.close,
                sma50 = cached.sma50,
                sma200 = cached.sma200,
                rsi = cached.rsi,
                peRatio = cached.peRatio,
                dividendYield = cached.dividendYield,
                aiMemo = cached.aiMemo
            )
        }

        // 2. Fetch from network with fallback to stale cache
        return try {
            val networkData = RetrofitInstance.apiService.getIndicators(ticker)
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
            networkData
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Return cached data if available even if stale
            if (cached != null) {
                MarketIndicatorsResponse(
                    ticker = cached.ticker,
                    close = cached.close,
                    sma50 = cached.sma50,
                    sma200 = cached.sma200,
                    rsi = cached.rsi,
                    peRatio = cached.peRatio,
                    dividendYield = cached.dividendYield,
                    aiMemo = "[OFFLINE CACHE] ${cached.aiMemo ?: "Historical price and technical indicators loaded from local Room DB."}"
                )
            } else {
                throw e
            }
        }
    }

    override suspend fun getTranscript(ticker: String, year: Int, quarter: Int) =
        RetrofitInstance.apiService.getTranscript(ticker, year, quarter)

    override suspend fun getRiskMesh(tickers: List<String>, weights: List<Double>) =
        RetrofitInstance.apiService.getRiskMesh(RiskMeshRequest(tickers, weights))

    override suspend fun getOptimization(tickers: List<String>, views: Map<String, Double>?) =
        RetrofitInstance.apiService.getOptimization(OptimizeRequest(tickers, views))

    override suspend fun runDebate(tickers: List<String>) =
        RetrofitInstance.apiService.runDebate(TickerListRequest(tickers))

    override suspend fun runConsult(query: String) =
        RetrofitInstance.apiService.runConsult(ConsultRequest(query))

    override suspend fun getStockChart(ticker: String, period: String) =
        RetrofitInstance.apiService.getStockChart(ticker, period)

    override suspend fun getNewsSentiment(ticker: String?) =
        RetrofitInstance.apiService.getNewsSentiment(ticker)

    override suspend fun searchScreener(query: String?, riskLevel: String?) =
        RetrofitInstance.apiService.searchScreener(query, riskLevel)

    override suspend fun runStressTest(tickers: List<String>, weights: List<Double>, scenarioId: String) =
        RetrofitInstance.apiService.runStressTest(StressTestRequest(tickers, weights, scenarioId))

    override suspend fun getFinnyReport() =
        RetrofitInstance.apiService.getFinnyReport()
}

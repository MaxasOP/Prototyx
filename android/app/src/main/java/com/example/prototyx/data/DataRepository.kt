package com.example.prototyx.data

import com.example.prototyx.data.model.*
import com.example.prototyx.data.network.RetrofitInstance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface DataRepository {
    val data: Flow<List<String>>
    
    suspend fun searchTickers(query: String? = null, sector: String? = null): List<TickerSearchResponse>
    suspend fun getIndicators(ticker: String): MarketIndicatorsResponse
    suspend fun getTranscript(ticker: String, year: Int = 2026, quarter: Int = 3): EarningsTranscriptResponse
    suspend fun getRiskMesh(tickers: List<String>, weights: List<Double>): RiskMeshResponse
    suspend fun getOptimization(tickers: List<String>, views: Map<String, Double>? = null): OptimizeResponse
    suspend fun runDebate(tickers: List<String>): DebateResponse
    suspend fun runConsult(query: String): ConsultResponse
}

class DefaultDataRepository : DataRepository {
    // Default active tickers for the client dashboard
    override val data: Flow<List<String>> = flow { 
        emit(listOf("TCS", "RELIANCE", "AAPL", "INFY")) 
    }

    override suspend fun searchTickers(query: String?, sector: String?): List<TickerSearchResponse> {
        return RetrofitInstance.apiService.searchTickers(query, sector)
    }

    override suspend fun getIndicators(ticker: String): MarketIndicatorsResponse {
        return RetrofitInstance.apiService.getIndicators(ticker)
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

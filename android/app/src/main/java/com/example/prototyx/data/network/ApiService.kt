package com.example.prototyx.data.network

import com.example.prototyx.data.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("api/tickers/search")
    suspend fun searchTickers(
        @Query("query") query: String? = null,
        @Query("sector") sector: String? = null
    ): List<TickerSearchResponse>

    @GET("api/market/indicators/{ticker}")
    suspend fun getIndicators(
        @Path("ticker") ticker: String
    ): MarketIndicatorsResponse

    @GET("api/earnings/transcript/{ticker}")
    suspend fun getTranscript(
        @Path("ticker") ticker: String,
        @Query("year") year: Int = 2026,
        @Query("quarter") quarter: Int = 3
    ): EarningsTranscriptResponse

    @POST("api/quant/risk-mesh")
    suspend fun getRiskMesh(
        @Body request: RiskMeshRequest
    ): RiskMeshResponse

    @POST("api/quant/optimize")
    suspend fun getOptimization(
        @Body request: OptimizeRequest
    ): OptimizeResponse

    @POST("api/agents/debate")
    suspend fun runDebate(
        @Body request: TickerListRequest
    ): DebateResponse

    @POST("api/agents/consult")
    suspend fun runConsult(
        @Body request: ConsultRequest
    ): ConsultResponse
}

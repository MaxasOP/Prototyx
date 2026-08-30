package com.example.prototyx.data.local.dao

import androidx.room.*
import com.example.prototyx.data.local.entity.Holding
import com.example.prototyx.data.local.entity.MarketIndicatorCache
import kotlinx.coroutines.flow.Flow

@Dao
interface PrototyxDao {
    @Query("SELECT * FROM holdings")
    fun getHoldingsFlow(): Flow<List<Holding>>

    @Query("SELECT * FROM holdings")
    suspend fun getAllHoldings(): List<Holding>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHoldings(holdings: List<Holding>)

    @Query("DELETE FROM holdings WHERE ticker = :ticker")
    suspend fun deleteHolding(ticker: String)

    @Query("SELECT * FROM market_indicator_cache WHERE ticker = :ticker")
    suspend fun getIndicator(ticker: String): MarketIndicatorCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndicator(indicator: MarketIndicatorCache)

    @Query("DELETE FROM holdings")
    suspend fun clearHoldings()

    @Query("DELETE FROM market_indicator_cache")
    suspend fun clearIndicatorCache()
}

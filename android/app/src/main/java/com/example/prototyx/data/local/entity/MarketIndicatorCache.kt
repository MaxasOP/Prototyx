package com.example.prototyx.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "market_indicator_cache")
data class MarketIndicatorCache(
    @PrimaryKey val ticker: String,
    val close: Double,
    val sma50: Double?,
    val sma200: Double?,
    val rsi: Double?,
    val peRatio: Double?,
    val dividendYield: Double?,
    val aiMemo: String?,
    val lastUpdated: Long = System.currentTimeMillis()
)

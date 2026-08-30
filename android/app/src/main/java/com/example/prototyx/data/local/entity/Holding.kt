package com.example.prototyx.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holdings")
data class Holding(
    @PrimaryKey val ticker: String,
    val weight: Float
)

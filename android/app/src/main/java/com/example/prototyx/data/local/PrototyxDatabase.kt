package com.example.prototyx.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.prototyx.data.local.dao.PrototyxDao
import com.example.prototyx.data.local.entity.Holding
import com.example.prototyx.data.local.entity.MarketIndicatorCache

@Database(
    entities = [Holding::class, MarketIndicatorCache::class],
    version = 1,
    exportSchema = false
)
abstract class PrototyxDatabase : RoomDatabase() {
    abstract fun prototyxDao(): PrototyxDao

    companion object {
        @Volatile
        private var INSTANCE: PrototyxDatabase? = null

        fun getDatabase(context: Context): PrototyxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrototyxDatabase::class.java,
                    "prototyx_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

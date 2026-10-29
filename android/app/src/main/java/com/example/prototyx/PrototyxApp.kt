package com.example.prototyx

import android.app.Application
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.DefaultDataRepository
import com.example.prototyx.data.local.PrototyxDatabase
import com.example.prototyx.data.security.AuthManager

class PrototyxApp : Application() {
    lateinit var repository: DataRepository
    lateinit var authManager: AuthManager
    lateinit var preferences: com.example.prototyx.data.AppPreferences

    override fun onCreate() {
        super.onCreate()
        val database = PrototyxDatabase.getDatabase(this)
        authManager = AuthManager(this)
        preferences = com.example.prototyx.data.AppPreferences(this)
        com.example.prototyx.notifications.AppNotifications.createChannels(this)
        com.example.prototyx.data.network.RetrofitInstance.init(authManager)
        repository = DefaultDataRepository(database.prototyxDao(),preferences)
    }
}

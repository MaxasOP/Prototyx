package com.example.prototyx.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

class AuthManager(context: Context) {
    var sessionExpired by mutableStateOf(false)
        private set
    private var sessionToken: String? = null
    private val sharedPreferences: SharedPreferences? = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.e("AuthManager", "Secure storage unavailable; login is limited to this session", e)
        null
    }

    fun saveToken(token: String) {
        sessionExpired = false
        sessionToken = token
        sharedPreferences?.edit()?.putString(KEY_TOKEN, token)?.apply()
    }

    fun getToken(): String? {
        return sessionToken ?: try { sharedPreferences?.getString(KEY_TOKEN, null) } catch (_: Exception) { null }
    }

    fun clearToken() {
        sessionExpired = true
        sessionToken = null
        sharedPreferences?.edit()?.remove(KEY_TOKEN)?.remove(KEY_DISCLAIMER_ACCEPTED)?.apply()
    }

    fun hasAcceptedDisclaimer(): Boolean {
        return try { sharedPreferences?.getBoolean(KEY_DISCLAIMER_ACCEPTED, false) ?: false } catch (_: Exception) { false }
    }

    fun setAcceptedDisclaimer(accepted: Boolean) {
        sharedPreferences?.edit()?.putBoolean(KEY_DISCLAIMER_ACCEPTED, accepted)?.apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
    }
}

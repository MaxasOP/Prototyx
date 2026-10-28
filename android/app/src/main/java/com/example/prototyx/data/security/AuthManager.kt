package com.example.prototyx.data.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AuthManager(context: Context) {
    private val sharedPreferences: SharedPreferences = try {
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
        Log.e("AuthManager", "Failed to initialize EncryptedSharedPreferences, falling back to standard prefs", e)
        // Delete corrupted preferences file if KeyStore issue occurred
        try {
            context.deleteSharedPreferences("secure_auth_prefs")
        } catch (_: Exception) {}
        context.getSharedPreferences("secure_auth_prefs_fallback", Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        try { sharedPreferences.edit().putString(KEY_TOKEN, token).apply() } catch (_: Exception) {}
    }

    fun getToken(): String? {
        return try { sharedPreferences.getString(KEY_TOKEN, null) } catch (_: Exception) { null }
    }

    fun clearToken() {
        try { sharedPreferences.edit().remove(KEY_TOKEN).apply() } catch (_: Exception) {}
    }

    fun hasAcceptedDisclaimer(): Boolean {
        return try { sharedPreferences.getBoolean(KEY_DISCLAIMER_ACCEPTED, true) } catch (_: Exception) { true }
    }

    fun setAcceptedDisclaimer(accepted: Boolean) {
        try { sharedPreferences.edit().putBoolean(KEY_DISCLAIMER_ACCEPTED, accepted).apply() } catch (_: Exception) {}
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
    }
}

package com.example.prototyx.ui.main

import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.fragment.app.FragmentActivity
import com.example.prototyx.PrototyxApp
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.UserProfile
import com.example.prototyx.data.network.ApiService
import com.example.prototyx.data.network.RetrofitInstance
import com.example.prototyx.data.security.BiometricAuthManager
import com.example.prototyx.notifications.AppNotifications
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.screens.AccountScreen
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

class AccountControlsTest {
    @get:Rule val rule=createComposeRule()
    private val app get()=InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PrototyxApp

    @Test fun accountProfileLoadsAndSavesWithoutChangingRealAccount() {
        val api=object:ApiService by RetrofitInstance.apiService {
            override suspend fun getAccount()=UserProfile("fixture@example.test","Fixture")
            override suspend fun updateAccount(body:Map<String,String>)=UserProfile("fixture@example.test",body["name"])
        }
        rule.setContent {PrototyxTheme {AccountScreen(MockDataRepository(),app.authManager,{},api=api)}}
        rule.waitUntil(5000) {rule.onAllNodesWithText("fixture@example.test").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("Save profile").performScrollTo().performClick()
        rule.onNodeWithText("Profile saved.").assertExists()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Delete account and data"))
        rule.onNodeWithText("Delete account and data").assertExists()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText("Sign out"))
        rule.onNodeWithText("Sign out").assertExists()
    }

    @Test fun notificationPostsAndHasAnInboxEntry() {
        assumeTrue(AppNotifications.allowed(app))
        val original=app.authManager.getToken()
        val ids=app.preferences.inbox.map{it.id}.toSet()
        try {
            rule.runOnIdle {app.authManager.saveToken(original ?: "local-notification-test");AppNotifications.post(app,true)}
            val entry=app.preferences.inbox.first {it.id !in ids}
            assertEquals("Test notification",entry.title)
            val manager=app.getSystemService(NotificationManager::class.java)
            assertTrue(manager.activeNotifications.any {it.id==entry.id.toInt()})
            assertNotNull(manager.getNotificationChannel(AppNotifications.CHANNEL))
            manager.cancel(entry.id.toInt())
            rule.runOnIdle {app.preferences.removeNotice(entry.id)}
        } finally {
            rule.runOnIdle {if(original!=null) app.authManager.saveToken(original) else app.authManager.clearToken()}
        }
    }

    @Test fun biometricSubsystemErrorsNeverUnlock() {
        var unlocked=false
        var failed=false
        val broken=object:ContextWrapper(app) {
            override fun getApplicationContext():Context=this
            override fun getSystemService(name:String):Any?=throw IllegalStateException("Fixture service failure")
        }
        rule.runOnIdle {BiometricAuthManager(broken).promptBiometricAuth(FragmentActivity(),onSuccess={unlocked=true},onError={failed=true})}
        assertFalse(unlocked)
        assertTrue(failed)
    }
}

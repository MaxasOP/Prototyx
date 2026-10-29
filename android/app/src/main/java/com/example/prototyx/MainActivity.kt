package com.example.prototyx

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.example.prototyx.data.security.BiometricAuthManager
import com.example.prototyx.notifications.AppNotifications
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private var resumed by mutableStateOf(false)
    private var unlocked by mutableStateOf(false)
    private var notificationEvent by mutableIntStateOf(0)
    override fun onResume() { super.onResume(); resumed=true }
    override fun onStop() { resumed=false; unlocked=false; super.onStop() }
    override fun onNewIntent(intent:Intent) { super.onNewIntent(intent); setIntent(intent); if(intent.getBooleanExtra("open_notifications",false)) notificationEvent++ }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        if(intent.getBooleanExtra("open_notifications",false)) notificationEvent++
        val app=application as PrototyxApp
        val biometrics=BiometricAuthManager(this)
        setContent {
            val prefs=app.preferences
            PrototyxTheme(darkTheme=prefs.darkTheme) {
                val dispatcher=rememberNavigationEventDispatcherOwner(parent=null)
                val scope=rememberCoroutineScope()
                var error by remember { mutableStateOf<String?>(null) }
                val hasToken=app.authManager.getToken()!=null
                val locked=hasToken && prefs.appLock && !unlocked
                SideEffect {
                    if(!prefs.appLock) unlocked=true
                    if(prefs.appLock) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                fun authenticate() {
                    biometrics.promptBiometricAuth(this@MainActivity,title="Unlock Prototyx",
                        subtitle="Use biometrics or your device credential",
                        onSuccess={unlocked=true;error=null},onError={error=it})
                }
                LaunchedEffect(resumed,locked) { if(resumed && locked) authenticate() }
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides dispatcher) {
                    Surface(Modifier.fillMaxSize()) {
                        if(locked) Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally) {
                            EditorialHeading("Your ledger is locked")
                            Spacer(Modifier.height(16.dp))
                            Text("Verify your identity to continue.")
                            error?.let { Text(it,color=MaterialTheme.colorScheme.error) }
                            Spacer(Modifier.height(20.dp))
                            PrimaryButton("Unlock",onClick={authenticate()})
                            TextButton(onClick={scope.launch {
                                runCatching { com.example.prototyx.data.network.RetrofitInstance.apiService.revokeSession() }
                                app.repository.logout();AppNotifications.clear(this@MainActivity);prefs.clearAccountData();app.authManager.clearToken()
                            }}) {Text("Sign out instead")}
                        } else MainNavigation(app.repository,app.authManager,notificationEvent,onNotificationConsumed={notificationEvent=0})
                    }
                }
            }
        }
    }
}

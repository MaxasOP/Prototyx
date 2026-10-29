package com.example.prototyx

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.theme.Signal
import com.example.prototyx.ui.components.FinnyMuseDots
import com.example.prototyx.ui.components.FinnyState
import com.example.prototyx.ui.screens.*
import com.example.prototyx.ui.voice.VoiceAssistantManager
import com.example.prototyx.ui.voice.VoiceCoPilotSheet
import kotlinx.coroutines.launch

sealed class BottomBarTab(val route: NavKey, val icon: ImageVector, val title: String) {
    object Dashboard : BottomBarTab(Main, Icons.Outlined.Home, "Portfolio")
    object ScreenerTab : BottomBarTab(Screener, Icons.Outlined.Search, "Screener")
    object Optimize : BottomBarTab(Optimizer, Icons.Outlined.Build, "Optimize")
    object Risk : BottomBarTab(RiskMesh, Icons.Outlined.Info, "Risk")
    object Debate : BottomBarTab(Committee, ResearchNavigationIcon, "Research")
    object Transcripts : BottomBarTab(Earnings, EarningsNavigationIcon, "Earnings")
}

@Composable
fun MainNavigation(
    repository: DataRepository,
    authManager: com.example.prototyx.data.security.AuthManager,
    notificationOpenEvent: Int = 0,
    onNotificationConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val preferences = (context.applicationContext as PrototyxApp).preferences
    val coroutineScope = rememberCoroutineScope()
    val voiceManager = remember { VoiceAssistantManager(context, coroutineScope) }
    var showVoiceSheet by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.destroy()
        }
    }

    val startDestination = if (authManager.getToken() != null) Main else Login
    val backStack = rememberNavBackStack(startDestination)
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if(authManager.getToken()!=null) {
            try { com.example.prototyx.data.network.RetrofitInstance.apiService.getAccount() }
            catch(e:kotlinx.coroutines.CancellationException) { throw e }
            catch(_:Exception) { /* Offline access remains available; a 401 clears the session. */ }
        }
    }
    androidx.compose.runtime.LaunchedEffect(notificationOpenEvent, authManager.getToken()) {
        if(notificationOpenEvent>0 && authManager.getToken()!=null) { backStack.clear();backStack.add(Main);backStack.add(Notifications);onNotificationConsumed() }
    }
    androidx.compose.runtime.LaunchedEffect(authManager.sessionExpired) {
        if (authManager.sessionExpired) {
            com.example.prototyx.notifications.AppNotifications.clear(context)
            preferences.clearAccountData()
            backStack.clear()
            backStack.add(Login)
        }
    }

    val tabs = listOf(
        BottomBarTab.Dashboard,
        BottomBarTab.ScreenerTab,
        BottomBarTab.Optimize,
        BottomBarTab.Risk,
        BottomBarTab.Debate,
        BottomBarTab.Transcripts
    )

    val currentScreen = backStack.lastOrNull() ?: Main

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceSheet = true
        } else {
            showVoiceSheet = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (currentScreen::class != Login::class) {
                Column(Modifier.statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                        if(currentScreen==Account || currentScreen==Notifications) {
                            IconButton(onClick={backStack.removeLastOrNull()}) { Icon(Icons.AutoMirrored.Outlined.ArrowBack,contentDescription="Back") }
                        } else Text("P/", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("PROTOTYX", fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { backStack.add(Notifications) }) {
                            BadgedBox(badge = { if(preferences.inbox.any { !it.read }) Badge() }) {
                                Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                            }
                        }
                        IconButton(onClick = { backStack.add(Account) }) { Icon(Icons.Outlined.AccountCircle,contentDescription = "Account and settings") }
                        TextButton(onClick = {
                            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) showVoiceSheet = true
                            else permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }) { Text("Ask Finny", fontSize = 12.sp) }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        },
        bottomBar = {
            if (currentScreen::class != Login::class) {
                NavigationBar(
                    modifier = Modifier.border(
                        1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f),
                        RoundedCornerShape(0.dp)
                    ),
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .98f),
                    tonalElevation = 0.dp
                ) {
                    tabs.forEach { tab ->
                        val isSelected = currentScreen::class == tab.route::class
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                backStack.clear()
                                backStack.add(tab.route)
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    letterSpacing = 0.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (showVoiceSheet) {
            VoiceCoPilotSheet(
                voiceManager = voiceManager,
                repository = repository,
                onDismiss = { showVoiceSheet = false }
            )
        }

        NavDisplay(

            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Login> {
                    LoginScreen(
                        repository = repository,
                        onLoginSuccess = { token ->
                            authManager.saveToken(token)
                            coroutineScope.launch {
                                // A new login restores the account snapshot before showing the portfolio.
                                try { repository.restoreHoldings() }
                                catch (e: kotlinx.coroutines.CancellationException) { throw e }
                                catch (_: Exception) { /* Dashboard exposes sync retry. */ }
                                backStack.clear()
                                backStack.add(Main)
                            }
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                entry<Account> { AccountScreen(repository,authManager,onSignedOut={backStack.clear();backStack.add(Login)},modifier=Modifier.padding(innerPadding)) }
                entry<Notifications> { NotificationsScreen(modifier=Modifier.padding(innerPadding)) }
                entry<Main> {
                    DashboardScreen(
                        repository = repository,
                        authManager = authManager,
                        onLogout = {
                            authManager.clearToken()
                            backStack.clear()
                            backStack.add(Login)
                        },
                        onVoiceTap = { showVoiceSheet = true },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                entry<Screener> {
                    ScreenerScreen(repository = repository, modifier = Modifier.padding(innerPadding))
                }
                entry<Optimizer> {
                    OptimizerScreen(repository = repository, modifier = Modifier.padding(innerPadding))
                }
                entry<RiskMesh> {
                    RiskMeshScreen(repository = repository, modifier = Modifier.padding(innerPadding))
                }
                entry<Committee> {
                    CommitteeScreen(repository = repository, modifier = Modifier.padding(innerPadding))
                }
                entry<Earnings> {
                    EarningsScreen(repository = repository, modifier = Modifier.padding(innerPadding))
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainNavigationPreview() {
    PrototyxTheme {
        MainNavigation(
            repository = MockDataRepository(),
            authManager = com.example.prototyx.data.security.AuthManager(LocalContext.current)
        )
    }
}

package com.example.prototyx

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
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

sealed class BottomBarTab(val route: NavKey, val icon: ImageVector, val title: String) {
    object Dashboard : BottomBarTab(Main, Icons.Outlined.Home, "Portfolio")
    object ScreenerTab : BottomBarTab(Screener, Icons.Outlined.Search, "Screener")
    object Optimize : BottomBarTab(Optimizer, Icons.Outlined.Settings, "Optimize")
    object Risk : BottomBarTab(RiskMesh, Icons.Outlined.Info, "Risk")
    object Debate : BottomBarTab(Committee, Icons.AutoMirrored.Outlined.List, "Research")
    object Transcripts : BottomBarTab(Earnings, Icons.Outlined.AccountBox, "Earnings")
}

@Composable
fun MainNavigation(
    repository: DataRepository,
    authManager: com.example.prototyx.data.security.AuthManager
) {
    val context = LocalContext.current
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
        floatingActionButton = {
            if (currentScreen::class != Login::class) {
                FloatingActionButton(
                    onClick = {
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.RECORD_AUDIO
                            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            showVoiceSheet = true
                        } else {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FinnyMuseDots(
                            state = when {
                                voiceManager.isListening -> FinnyState.LISTENING
                                voiceManager.isSpeaking -> FinnyState.SPEAKING
                                voiceManager.isThinking -> FinnyState.THINKING
                                else -> FinnyState.IDLE
                            },
                            dotSize = 5.dp,
                            dotSpacing = 3.dp
                        )
                        Text(
                            text = "FINNY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (currentScreen::class != Login::class) {
                NavigationBar(
                    modifier = Modifier.border(
                        1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .7f),
                        RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
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
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title.uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Signal.copy(alpha = .12f),
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
                            backStack.clear()
                            backStack.add(Main)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
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

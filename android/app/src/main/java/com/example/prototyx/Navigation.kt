package com.example.prototyx

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import com.example.prototyx.theme.PrototyxTheme
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.DefaultDataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.ui.screens.*

sealed class BottomBarTab(val route: NavKey, val icon: ImageVector, val title: String) {
    object Dashboard : BottomBarTab(Main, Icons.Outlined.Home, "Terminal")
    object Optimize : BottomBarTab(Optimizer, Icons.Outlined.Settings, "Optimize")
    object Risk : BottomBarTab(RiskMesh, Icons.Outlined.Info, "Risk Mesh")
    object Debate : BottomBarTab(Committee, Icons.AutoMirrored.Outlined.List, "Committee")
    object Transcripts : BottomBarTab(Earnings, Icons.Outlined.Search, "Earnings")
}

@Composable
fun MainNavigation(
    repository: DataRepository,
    authManager: com.example.prototyx.data.security.AuthManager
) {
    val startDestination = if (authManager.getToken() != null) Main else Login
    val backStack = rememberNavBackStack(startDestination)
    
    val tabs = listOf(
        BottomBarTab.Dashboard,
        BottomBarTab.Optimize,
        BottomBarTab.Risk,
        BottomBarTab.Debate,
        BottomBarTab.Transcripts
    )

    Scaffold(
        bottomBar = {
            val currentScreen = backStack.lastOrNull() ?: Main
            if (currentScreen::class != Login::class) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp
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
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ) 
                            },
                            label = { 
                                Text(
                                    text = tab.title.uppercase(), 
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                                    letterSpacing = 0.5.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ) 
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
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
                        onLogout = {
                            authManager.clearToken()
                            backStack.clear()
                            backStack.add(Login)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
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
            authManager = com.example.prototyx.data.security.AuthManager(androidx.compose.ui.platform.LocalContext.current)
        )
    }
}

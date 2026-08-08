package com.example.prototyx

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import com.example.prototyx.theme.PrototyxTheme
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.prototyx.data.DefaultDataRepository
import com.example.prototyx.ui.screens.*

sealed class BottomBarTab(val route: NavKey, val icon: ImageVector, val title: String) {
    object Dashboard : BottomBarTab(Main, Icons.Default.Home, "Dashboard")
    object Optimize : BottomBarTab(Optimizer, Icons.Default.Settings, "Optimize")
    object Risk : BottomBarTab(RiskMesh, Icons.Default.Info, "Risk Mesh")
    object Debate : BottomBarTab(Committee, Icons.Default.List, "AI Committee")
    object Transcripts : BottomBarTab(Earnings, Icons.Default.Search, "Earnings")
}

@Composable
fun MainNavigation() {
    val repository = DefaultDataRepository()
    val backStack = rememberNavBackStack(Main)
    
    // List of bottom tabs
    val tabs = listOf(
        BottomBarTab.Dashboard,
        BottomBarTab.Optimize,
        BottomBarTab.Risk,
        BottomBarTab.Debate,
        BottomBarTab.Transcripts
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentScreen = backStack.lastOrNull() ?: Main
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentScreen::class == tab.route::class,
                        onClick = {
                            // Empty backstack and add new route to prevent stack buildup
                            while (backStack.size > 0) {
                                backStack.removeLastOrNull()
                            }
                            backStack.add(tab.route)
                        },
                        icon = { Icon(imageVector = tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, fontSize = 10.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Main> {
                    DashboardScreen(repository = repository, modifier = Modifier.padding(innerPadding))
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
        MainNavigation()
    }
}

package com.example.prototyx.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.*
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.theme.Mint
import com.example.prototyx.theme.Coral
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch

data class PresetBasket(
    val title: String,
    val icon: String,
    val subtitle: String,
    val holdings: Map<String, Float>
)

data class PortfolioHealthReport(
    val score: Int,
    val status: String,
    val statusColor: Color,
    val diagnosis: String
)

@Composable
fun DashboardScreen(
    repository: DataRepository,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    authManager: com.example.prototyx.data.security.AuthManager? = null,
    onVoiceTap: () -> Unit = {}
) {
    var showDisclaimer by remember { mutableStateOf(authManager?.hasAcceptedDisclaimer() == false) }

    if (showDisclaimer) {
        FinancialDisclaimerDialog(
            onAccept = {
                authManager?.setAcceptedDisclaimer(true)
                showDisclaimer = false
            }
        )
    }
    val holdings by repository.holdings.collectAsState(initial = emptyMap())
    val coroutineScope = rememberCoroutineScope()
    var selectedTicker by remember { mutableStateOf("") }
    var tickerData by remember { mutableStateOf<MarketIndicatorsResponse?>(null) }
    var chartData by remember { mutableStateOf<StockChartResponse?>(null) }
    var newsFeed by remember { mutableStateOf<List<NewsSentimentItem>>(emptyList()) }
    var finnyReport by remember { mutableStateOf<FinnyStrategyReport?>(null) }

    var userMode by remember { mutableStateOf(UserExperienceMode.PRO) }
    var isLoading by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var syncError by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var searchWeight by remember { mutableStateOf("") }

    val activeTickers = remember(holdings) { holdings.keys.toList() }
    val currentWeights = remember(holdings) { holdings.values.toList() }

    val defaultColors = listOf(
        Color(0xFF111111), Color(0xFF787774), Color(0xFFC0C0C0), Color(0xFFE1F3FE),
        Color(0xFF1F6C9F), Color(0xFF4CAF50), Color(0xFF9C27B0), Color(0xFFFF9800)
    )

    LaunchedEffect(Unit) {
        finnyReport = repository.getFinnyReport()
        newsFeed = repository.getNewsSentiment()
    }

    LaunchedEffect(selectedTicker) {
        if (selectedTicker.isNotEmpty()) {
            isLoading = true
            searchError = null
            try {
                tickerData = repository.getIndicators(selectedTicker)
                chartData = repository.getStockChart(selectedTicker)
            } catch (e: Exception) {
                searchError = "Could not load $selectedTicker: ${e.message ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(holdings) {
        if (selectedTicker.isEmpty() && holdings.isNotEmpty()) {
            selectedTicker = holdings.keys.first()
        }
    }

    val demoBaskets = remember {
        listOf(
            PresetBasket("Technology", "", "Growth", mapOf("NVDA" to 0.35f, "AAPL" to 0.35f, "MSFT" to 0.30f)),
            PresetBasket("Core India", "", "Balanced", mapOf("RELIANCE" to 0.40f, "TCS" to 0.30f, "HDFCBANK" to 0.30f)),
            PresetBasket("Defensive", "", "Income", mapOf("ITC" to 0.40f, "HINDUNILVR" to 0.35f, "TCS" to 0.25f))
        )
    }

    val healthReport = remember(holdings) {
        if (holdings.isEmpty()) {
            PortfolioHealthReport(0, "NO HOLDINGS", Color.Gray, "Add an allocation below or begin with a model portfolio.")
        } else {
            val totalAssets = holdings.size
            val totalWeight = holdings.values.sum()
            val maxWeight = if (totalWeight > 0) (holdings.values.maxOrNull() ?: 0f) / totalWeight else 1f

            var score = 50
            if (totalAssets >= 3) score += 25
            else if (totalAssets == 2) score += 12

            if (maxWeight < 0.45f) score += 20
            else if (maxWeight > 0.60f) score -= 25

            score = score.coerceIn(25, 96)

            val (status, color, diagnosis) = when {
                score >= 80 -> Triple(
                    "ROBUST ALLOCATION",
                    Mint,
                    "Portfolio is resilient across $totalAssets holdings. No single stock exceeds 45% capital allocation."
                )
                score >= 60 -> Triple(
                    "MODERATE CONCENTRATION",
                    com.example.prototyx.theme.Amber,
                    "Top holding represents ${(maxWeight * 100).toInt()}% of portfolio. Rebalance recommended to limit sector drawdown."
                )
                else -> Triple(
                    "HIGH VULNERABILITY",
                    Coral,
                    "Elevated single-asset concentration (${(maxWeight * 100).toInt()}%). Add defensive counterweights or hedge exposure."
                )
            }
            PortfolioHealthReport(score, status, color, diagnosis)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Hero Header
        item(key = "heading") {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp)
            ) {
                MarketAtmosphere(
                    modifier = Modifier.fillMaxWidth(.64f).height(112.dp).align(Alignment.BottomEnd)
                )
                Column(modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(.72f)) {
                    MetadataLabel(text = "PROTOTYX · PRIVATE WEALTH")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Portfolio", style = MaterialTheme.typography.headlineLarge)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Allocation, risk and relevant market context", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isSyncing = true
                                syncError = null
                                try {
                                    repository.syncWithCloud()
                                } catch (e: Exception) {
                                    syncError = "Sync failed: ${e.message}"
                                } finally {
                                    isSyncing = false
                                }
                            }
                        },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync with Cloud",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.logout()
                                onLogout()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // Mode Toggle (Simple vs Pro)
        item(key = "mode_toggle") {
            ModeToggleBar(
                currentMode = userMode,
                onModeChanged = { userMode = it }
            )
        }

        // FINNY Virtual Agent Card
        item(key = "finny_agent") {
            finnyReport?.let { report ->
                FinnyVirtualAgentCard(
                    healthScore = report.portfolioHealthScore,
                    briefingSummary = report.dailyBriefingSummary,
                    onVoiceTap = onVoiceTap,
                    state = FinnyState.IDLE
                )
            }
        }

        // Quick-Test Baskets
        item(key = "presets_row") {
            Column {
                SectionHeader(title = "Model portfolios", supportingText = "Replace the current allocation with a reference strategy")
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    demoBaskets.forEach { basket ->
                        PresetBasketChip(
                            basket = basket,
                            onClick = {
                                coroutineScope.launch {
                                    holdings.keys.forEach { oldTicker ->
                                        repository.removeHolding(oldTicker)
                                    }
                                    basket.holdings.forEach { (ticker, weight) ->
                                        repository.updateHolding(ticker, weight)
                                    }
                                    selectedTicker = basket.holdings.keys.first()
                                }
                            }
                        )
                    }
                }
            }
        }

        // Portfolio Health Meter or Simple Risk Gauge
        item(key = "health_scorecard") {
            if (userMode == UserExperienceMode.SIMPLE) {
                SimpleRiskGauge(status = healthReport.status)
            } else {
                PortfolioHealthCard(report = healthReport)
            }
        }

        // Interactive Stock Charting Engine
        if (selectedTicker.isNotEmpty()) {
            item(key = "interactive_chart") {
                chartData?.let { data ->
                    StockChartView(
                        ticker = data.ticker,
                        pricePoints = data.pricePoints
                    )
                }
            }
        }

        // Search & Discovery
        item(key = "search_card") {
            DoubleBezelCard {
                SectionHeader(title = "Edit allocation", supportingText = "Set a decimal weight, or enter zero to remove")
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Ticker (e.g. NVDA)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = searchWeight,
                        onValueChange = { searchWeight = it },
                        label = { Text("Weight (e.g. 0.2)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                PrimaryButton(
                    text = "Apply allocation",
                    onClick = {
                        val ticker = searchQuery.trim().uppercase()
                        val weight = searchWeight.toDoubleOrNull()?.toFloat() ?: 0f
                        if (ticker.isNotEmpty()) {
                            coroutineScope.launch {
                                if (weight > 0) {
                                    repository.updateHolding(ticker, weight)
                                } else {
                                    repository.removeHolding(ticker)
                                }
                            }
                            selectedTicker = ticker
                            searchQuery = ""
                            searchWeight = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // News Sentiment Stream
        if (newsFeed.isNotEmpty()) {
            item(key = "news_stream_header") {
                SectionHeader(title = "Portfolio news", supportingText = "Developments that may affect current holdings")
            }
            items(newsFeed) { news ->
                NewsSentimentCard(item = news, userMode = userMode)
            }
        }
    }
}

@Composable
fun TechnicalMetricItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun PresetBasketChip(
    basket: PresetBasket,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = basket.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = basket.subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PortfolioHealthCard(report: PortfolioHealthReport) {
    DoubleBezelCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "Portfolio resilience")
            Surface(
                color = report.statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = report.status,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = report.statusColor
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${report.score}",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/100",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                MetadataLabel(text = "STRESS & RESILIENCE SCORE")
            }

            AnimatedScoreRing(score = report.score, color = report.statusColor)
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = report.diagnosis,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    PrototyxTheme {
        DashboardScreen(repository = MockDataRepository(), onLogout = {})
    }
}

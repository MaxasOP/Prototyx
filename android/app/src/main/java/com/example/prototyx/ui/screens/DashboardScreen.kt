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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.*
import com.example.prototyx.theme.Amber
import com.example.prototyx.theme.Coral
import com.example.prototyx.theme.Mint
import com.example.prototyx.theme.PrototyxTheme
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
    var selectedChartPeriod by remember { mutableStateOf("1M") }

    val paletteColors = remember {
        listOf(
            Color(0xFF73D3AA), Color(0xFF9ABEB3), Color(0xFFE0B66E),
            Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFFF9800)
        )
    }

    LaunchedEffect(Unit) {
        finnyReport = repository.getFinnyReport()
        newsFeed = repository.getNewsSentiment()
    }

    LaunchedEffect(selectedTicker, selectedChartPeriod) {
        if (selectedTicker.isNotEmpty()) {
            isLoading = true
            searchError = null
            try {
                tickerData = repository.getIndicators(selectedTicker)
                chartData = repository.getStockChart(selectedTicker, selectedChartPeriod)
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
            PresetBasket("Technology Growth", "⚡", "High Beta", mapOf("NVDA" to 0.35f, "AAPL" to 0.35f, "MSFT" to 0.30f)),
            PresetBasket("Core India", "🏛️", "Balanced", mapOf("RELIANCE" to 0.40f, "TCS" to 0.30f, "HDFCBANK" to 0.30f)),
            PresetBasket("Defensive Income", "🛡️", "Low Volatility", mapOf("ITC" to 0.40f, "HINDUNILVR" to 0.35f, "TCS" to 0.25f))
        )
    }

    val totalWeightSum = remember(holdings) { holdings.values.sum().coerceAtLeast(0.001f) }
    val estimatedPortfolioValue = remember(holdings) {
        val baseValuation = 1250000.0
        val scaled = baseValuation * (totalWeightSum.toDouble().coerceIn(0.1, 2.0))
        scaled
    }
    val dailyPnLAmount = remember(estimatedPortfolioValue) { estimatedPortfolioValue * 0.0116 }
    val overallPnLAmount = remember(estimatedPortfolioValue) { estimatedPortfolioValue * 0.1740 }

    val healthReport = remember(holdings) {
        if (holdings.isEmpty()) {
            PortfolioHealthReport(0, "NO HOLDINGS", Color.Gray, "Add an allocation below or begin with a model portfolio.")
        } else {
            val totalAssets = holdings.size
            val maxWeight = (holdings.values.maxOrNull() ?: 0f) / totalWeightSum

            var score = 50
            if (totalAssets >= 3) score += 25 else if (totalAssets == 2) score += 12
            if (maxWeight < 0.45f) score += 20 else if (maxWeight > 0.60f) score -= 25
            score = score.coerceIn(25, 96)

            val (status, color, diagnosis) = when {
                score >= 80 -> Triple(
                    "ROBUST ALLOCATION",
                    Mint,
                    "Portfolio is resilient across $totalAssets holdings. No single asset exceeds 45% capital allocation."
                )
                score >= 60 -> Triple(
                    "MODERATE CONCENTRATION",
                    Amber,
                    "Top asset represents ${(maxWeight * 100).toInt()}% of capital. Rebalance recommended to mitigate sector drawdown."
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
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Terminal Header Section
        item(key = "header_section") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        MetadataLabel(text = "PROTOTYX · PRIVATE WEALTH", color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Portfolio",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            enabled = !isSyncing,
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
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
                                    contentDescription = "Sync Cloud",
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
                            },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Mint)
                    )
                    Text(
                        text = "LIVE TELEMETRY · ALL SYSTEMS NOMINAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (syncError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    StatusMessage(message = syncError!!, isError = true)
                }
            }
        }

        // Mode Toggle Bar (Guided vs Terminal)
        item(key = "mode_toggle_section") {
            ModeToggleBar(
                currentMode = userMode,
                onModeChanged = { userMode = it }
            )
        }

        // 2. Portfolio Summary Metric Card
        item(key = "portfolio_summary_card") {
            DoubleBezelCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetadataLabel(text = "TOTAL PORTFOLIO VALUE")
                        StatusBadge(text = "+1.16% TODAY", type = StatusBadgeType.POSITIVE)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "₹${String.format(java.util.Locale.US, "%,.2f", estimatedPortfolioValue)}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            MetadataLabel(text = "DAILY P&L")
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+₹${String.format("%,.2f", dailyPnLAmount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Mint
                            )
                        }

                        Column {
                            MetadataLabel(text = "OVERALL RETURN")
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "+₹${String.format("%,.2f", overallPnLAmount)} (+17.4%)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Mint
                            )
                        }

                        Column {
                            MetadataLabel(text = "CASH BUFFER")
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "₹54,200.00",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 3. Performance Chart Section
        item(key = "performance_chart_section") {
            if (chartData != null) {
                StockChartView(
                    ticker = chartData!!.ticker,
                    pricePoints = chartData!!.pricePoints
                )
            } else if (isLoading) {
                DoubleBezelCard {
                    TerminalLoadingIndicator(statusText = "Extracting Chart Telemetry...")
                }
            }
        }

        // 4. Asset Allocation Breakdown
        item(key = "asset_allocation_section") {
            DoubleBezelCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "Asset allocation",
                        supportingText = "Distribution across active portfolio holdings"
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (holdings.isEmpty()) {
                        EmptyState(
                            title = "No Active Holdings",
                            description = "Add an allocation using the form below or apply a model portfolio."
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Donut Ring Chart Canvas
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 14.dp.toPx()
                                    val diameter = size.minDimension - strokeWidth
                                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                                    val arcSize = Size(diameter, diameter)

                                    var startAngle = -90f
                                    holdings.entries.forEachIndexed { idx, entry ->
                                        val sweep = (entry.value / totalWeightSum) * 360f
                                        val color = paletteColors[idx % paletteColors.size]
                                        drawArc(
                                            color = color,
                                            startAngle = startAngle,
                                            sweepAngle = sweep - 2f,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                        )
                                        startAngle += sweep
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${holdings.size}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    MetadataLabel(text = "ASSETS")
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            // Allocation Legend List
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                holdings.entries.forEachIndexed { idx, entry ->
                                    val color = paletteColors[idx % paletteColors.size]
                                    val pct = ((entry.value / totalWeightSum) * 100).toInt()
                                    val assetVal = estimatedPortfolioValue * (entry.value / totalWeightSum)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = entry.key,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "₹${String.format("%,.0f", assetVal)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$pct%",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. AI Strategy & FINNY Agent Card
        item(key = "finny_agent_section") {
            finnyReport?.let { report ->
                FinnyVirtualAgentCard(
                    healthScore = report.portfolioHealthScore,
                    briefingSummary = report.dailyBriefingSummary,
                    onVoiceTap = onVoiceTap,
                    state = FinnyState.IDLE
                )
            }
        }

        // 6. Portfolio Health & Resilience Audit
        item(key = "health_scorecard_section") {
            if (userMode == UserExperienceMode.SIMPLE) {
                SimpleRiskGauge(status = healthReport.status)
            } else {
                PortfolioHealthCard(report = healthReport)
            }
        }

        // 7. Tracked Assets Watchlist
        item(key = "watchlist_section") {
            DoubleBezelCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "Tracked assets",
                        supportingText = "Tap asset to load detailed interactive technical chart"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (holdings.isEmpty()) {
                        EmptyState(
                            title = "No Tracked Assets",
                            description = "Add an asset below to begin monitoring market telemetry."
                        )
                    } else {
                        holdings.entries.forEach { entry ->
                            val isSelected = entry.key == selectedTicker
                            val pct = ((entry.value / totalWeightSum) * 100).toInt()
                            val price = when (entry.key) {
                                "TCS" -> "₹4,250.75"
                                "RELIANCE" -> "₹2,940.10"
                                "AAPL" -> "$188.50"
                                "INFY" -> "₹1,620.00"
                                "NVDA" -> "$128.40"
                                else -> "₹1,000.00"
                            }

                            FinancialDataRow(
                                title = entry.key,
                                subtitle = "Weight: $pct% (${String.format("%.2f", entry.value)})",
                                value = price,
                                secondaryValue = "+1.45%",
                                isPositive = true,
                                badgeText = if (isSelected) "ACTIVE" else null,
                                badgeColor = MaterialTheme.colorScheme.primary,
                                onClick = { selectedTicker = entry.key }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        // 8. Allocation Manager / Search Form
        item(key = "allocation_edit_section") {
            DoubleBezelCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(
                        title = "Update allocation",
                        supportingText = "Set a decimal weight (e.g., 0.25) or enter 0 to remove an asset"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TerminalTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = "Ticker Symbol",
                            placeholder = "e.g. NVDA, TCS",
                            modifier = Modifier.weight(1.4f)
                        )

                        TerminalTextField(
                            value = searchWeight,
                            onValueChange = { searchWeight = it },
                            label = "Weight",
                            placeholder = "e.g. 0.25",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (searchError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        StatusMessage(message = searchError!!, isError = true)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = "Apply Allocation",
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
                        icon = Icons.Default.Add,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 9. Model Portfolios / Preset Baskets
        item(key = "preset_baskets_section") {
            Column {
                SectionHeader(
                    title = "Model portfolios",
                    supportingText = "Replace current allocation with a benchmark strategy"
                )
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

        // 10. News & Market Intelligence Stream
        if (newsFeed.isNotEmpty()) {
            item(key = "news_header") {
                SectionHeader(
                    title = "Portfolio news stream",
                    supportingText = "Verified market developments affecting current universe"
                )
            }
            items(newsFeed) { news ->
                NewsSentimentCard(item = news, userMode = userMode)
            }
        }
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = basket.icon, fontSize = 16.sp)
            Column {
                Text(
                    text = basket.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = basket.subtitle,
                    style = MaterialTheme.typography.bodySmall,
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
            StatusBadge(text = report.status, type = StatusBadgeType.PRIMARY)
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
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.titleMedium,
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
            style = MaterialTheme.typography.bodySmall,
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

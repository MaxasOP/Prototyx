package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.RiskMeshResponse
import com.example.prototyx.data.model.StressTestResult
import com.example.prototyx.theme.Amber
import com.example.prototyx.theme.Coral
import com.example.prototyx.theme.Mint
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun RiskMeshScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val portfolioHoldings by repository.holdings.collectAsState(initial = emptyMap())

    var meshHoldings by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var tickerInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }

    var riskMeshResult by remember { mutableStateOf<RiskMeshResponse?>(null) }
    var stressTestResult by remember { mutableStateOf<StressTestResult?>(null) }
    var activeScenarioId by remember { mutableStateOf("rate_hike") }
    var isLoading by remember { mutableStateOf(false) }
    var isStressLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(portfolioHoldings) {
        if (meshHoldings.isEmpty() && portfolioHoldings.isNotEmpty()) {
            meshHoldings = portfolioHoldings.mapValues { it.value.toDouble() }
        }
    }

    // Auto-calculate risk mesh on initial load if holdings exist
    LaunchedEffect(meshHoldings) {
        if (riskMeshResult == null && meshHoldings.isNotEmpty()) {
            try {
                riskMeshResult = repository.getRiskMesh(meshHoldings.keys.toList(), meshHoldings.values.toList())
            } catch (e: Exception) {
                // Silently fallback to manual calculate
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Terminal Header
            item(key = "header") {
                ScreenHeader(
                    category = "PROTOTYX · QUANTITATIVE RISK MESH",
                    title = "Portfolio Risk",
                    subtitle = "Systematic risk exposure, correlation matrix, and macro scenario stress tests"
                )
            }

            // 2. High Priority Risk Overview
            riskMeshResult?.let { res ->
                item(key = "risk_overview_card") {
                    DoubleBezelCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader(title = "Risk parameters")
                                StatusBadge(
                                    text = getNEIDesc(res.netExposureIndex),
                                    type = if (res.netExposureIndex > 0.5) StatusBadgeType.NEGATIVE else StatusBadgeType.POSITIVE
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricCard(
                                    label = "NET EXPOSURE INDEX",
                                    value = String.format(Locale.US, "%.2f", res.netExposureIndex),
                                    supportingText = "Unhedged Market Exposure",
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                MetricCard(
                                    label = "PORTFOLIO BETA",
                                    value = String.format(Locale.US, "%.2f", res.portfolioBeta),
                                    supportingText = getBetaDesc(res.portfolioBeta),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            res.aiRiskAudit?.let { audit ->
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = "Audit",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "AI QUANTITATIVE RISK AUDIT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = audit,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. Scenario Stress Testing Section
            item(key = "stress_test_section") {
                DoubleBezelCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Macro scenario stress testing",
                            supportingText = "Simulate historical & hypothetical shocks to estimate portfolio drawdown"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Scenario Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val scenarios = listOf(
                                "rate_hike" to "📈 Fed Rate Hike (+50 bps)",
                                "oil_spike" to "🛢️ Crude Oil Shock (+25%)",
                                "tech_selloff" to "💻 Tech Sell-Off (-15%)"
                            )

                            scenarios.forEach { (id, label) ->
                                TerminalFilterChip(
                                    selected = (activeScenarioId == id),
                                    onClick = {
                                        activeScenarioId = id
                                        coroutineScope.launch {
                                            isStressLoading = true
                                            val tickers = if (meshHoldings.isNotEmpty()) meshHoldings.keys.toList() else listOf("TCS", "RELIANCE")
                                            val weights = if (meshHoldings.isNotEmpty()) meshHoldings.values.toList() else listOf(0.5, 0.5)
                                            stressTestResult = repository.runStressTest(tickers, weights, id)
                                            isStressLoading = false
                                        }
                                    },
                                    label = label
                                )
                            }
                        }

                        if (isStressLoading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            TerminalLoadingIndicator(statusText = "Simulating Stressed Portfolio Value...")
                        }

                        stressTestResult?.let { res ->
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = res.scenarioName.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                StatusBadge(text = "SIMULATED SHOCK", type = StatusBadgeType.WARNING)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricCard(
                                    label = "RETURN IMPACT",
                                    value = "${String.format(Locale.US, "%.1f", res.portfolioReturnChange * 100)}%",
                                    isPositiveChange = res.portfolioReturnChange >= 0,
                                    changeText = "SHOCK",
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                MetricCard(
                                    label = "STRESSED VaR",
                                    value = "${String.format(Locale.US, "%.1f", res.stressedVaR * 100)}%",
                                    supportingText = "95% Confidence",
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                MetricCard(
                                    label = "MAX DRAWDOWN",
                                    value = "${String.format(Locale.US, "%.1f", res.stressedDrawdown * 100)}%",
                                    supportingText = "Peak-to-Trough",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "💡 FINNY Strategy Verdict: ${res.plainEnglishVerdict}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Asset Correlation Heatmap Matrix
            riskMeshResult?.let { res ->
                item(key = "correlation_heatmap_section") {
                    DoubleBezelCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(
                                title = "Correlation matrix heatmap",
                                supportingText = "Asset pair correlations (-1.0 inversely correlated to +1.0 highly correlated)"
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            CorrelationHeatmapMatrix(
                                tickers = res.tickers,
                                matrix = res.correlationMatrix
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Heatmap Legend Scale
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(Coral))
                                    Text("Inverse (-0.5)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                                    Text("Uncorrelated (0.0)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.surface))
                                    Text("Correlated (+0.8)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Concentration & Redundancy Warnings
                if (res.redundantExposures.isNotEmpty()) {
                    item(key = "concentration_alerts_section") {
                        DoubleBezelCard(
                            borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SectionHeader(
                                    title = "Concentration alerts",
                                    supportingText = "Detected high correlation asset pairs reducing diversification"
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                res.redundantExposures.forEach { alert ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f))
                                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Warning,
                                            contentDescription = "Alert",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = alert.warning,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 18.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            // 5. Asset Basket Configuration Input Form
            item(key = "asset_config_section") {
                DoubleBezelCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Custom asset basket",
                            supportingText = "Configure assets & weights to calculate net risk parameters"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TerminalTextField(
                                value = tickerInput,
                                onValueChange = { tickerInput = it },
                                label = "Asset Symbol",
                                placeholder = "e.g. NVDA, TCS",
                                modifier = Modifier.weight(1.4f)
                            )

                            TerminalTextField(
                                value = weightInput,
                                onValueChange = { weightInput = it },
                                label = "Weight",
                                placeholder = "e.g. 0.35",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            StatusMessage(message = errorMessage!!, isError = true)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecondaryButton(
                                text = "Add / Update",
                                onClick = {
                                    val ticker = tickerInput.trim().uppercase()
                                    val weight = weightInput.toDoubleOrNull() ?: 0.0
                                    if (ticker.isNotEmpty()) {
                                        val newHoldings = meshHoldings.toMutableMap()
                                        if (weight > 0) newHoldings[ticker] = weight else newHoldings.remove(ticker)
                                        meshHoldings = newHoldings
                                        tickerInput = ""
                                        weightInput = ""
                                    }
                                },
                                icon = Icons.Default.Add,
                                modifier = Modifier.weight(1f)
                            )

                            PrimaryButton(
                                text = "Calculate Risk",
                                onClick = {
                                    coroutineScope.launch {
                                        if (meshHoldings.isEmpty()) {
                                            errorMessage = "Please add at least one asset to the basket."
                                            return@launch
                                        }
                                        isLoading = true
                                        errorMessage = null
                                        riskMeshResult = null
                                        try {
                                            riskMeshResult = repository.getRiskMesh(meshHoldings.keys.toList(), meshHoldings.values.toList())
                                        } catch (e: Exception) {
                                            errorMessage = "Risk engine connection failure: ${e.message ?: "Server unreachable"}"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                isLoading = isLoading,
                                modifier = Modifier.weight(1.4f)
                            )
                        }

                        if (meshHoldings.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            MetadataLabel(text = "Current Basket: ${meshHoldings.keys.joinToString(", ")}")
                        }
                    }
                }
            }

            // 6. Quantitative Methodology Disclaimer
            item(key = "risk_disclaimer_section") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Disclaimer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Methodology Disclaimer: Risk parameters, Value at Risk (VaR), and stress test drawdowns are probabilistic statistical estimates calculated from trailing historical return series. They do not guarantee future portfolio outcomes or protect against extreme market black-swan events.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * CorrelationHeatmapMatrix: Grid component displaying asset pair correlation coefficients.
 */
@Composable
fun CorrelationHeatmapMatrix(
    tickers: List<String>,
    matrix: Map<String, Map<String, Double>>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header Row (Column Asset Tickers)
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f))
            tickers.forEach { ticker ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = ticker,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Matrix Rows
        tickers.forEach { rowTicker ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = rowTicker,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                tickers.forEach { colTicker ->
                    val corr = matrix[rowTicker]?.get(colTicker) ?: 1.0
                    val cellColor = getHeatmapCellColor(corr)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.2f)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(cellColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.2f", corr),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (corr >= 0.8 || corr <= -0.4) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun getHeatmapCellColor(corr: Double): Color {
    return when {
        corr >= 0.8 -> Color(0xFF1B3D2F)
        corr >= 0.5 -> Color(0xFF264235)
        corr >= 0.2 -> Color(0xFF1A2A23)
        corr <= -0.5 -> Color(0xFF4A2024)
        corr <= -0.1 -> Color(0xFF321A1D)
        else -> Color(0xFF17251F)
    }
}

private fun getNEIDesc(nei: Double): String = if (nei > 0.5) "CONCENTRATED RISK" else "OPTIMALLY DIVERSIFIED"
private fun getBetaDesc(beta: Double): String = if (beta > 1.2) "High Volatility" else if (beta < 0.8) "Low Beta / Defensive" else "Market Neutral Beta"

@Preview(showBackground = true)
@Composable
fun RiskMeshScreenPreview() {
    PrototyxTheme {
        RiskMeshScreen(repository = MockDataRepository())
    }
}

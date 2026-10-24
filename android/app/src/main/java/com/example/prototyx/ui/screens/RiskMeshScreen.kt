package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.RiskMeshResponse
import com.example.prototyx.data.model.StressTestResult
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch

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
    var isLoading by remember { mutableStateOf(false) }
    var isStressLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(portfolioHoldings) {
        if (meshHoldings.isEmpty() && portfolioHoldings.isNotEmpty()) {
            meshHoldings = portfolioHoldings.mapValues { it.value.toDouble() }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item(key = "heading") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    EditorialHeading(text = "Portfolio risk")
                    Spacer(modifier = Modifier.height(8.dp))
                    MetadataLabel(text = "EXPOSURE, CORRELATION AND DOWNSIDE SCENARIOS")
                }
            }
        }

        // Stress Testing Simulator Section
        item(key = "stress_test_card") {
            DoubleBezelCard {
                MetadataLabel(text = "SCENARIO ANALYSIS")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Estimate how macro shocks may affect value at risk and drawdown.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val scenarios = listOf(
                        "rate_hike" to "📈 Fed Rate Hike",
                        "oil_spike" to "🛢️ Oil Shock",
                        "tech_selloff" to "💻 Tech Sell-off"
                    )

                    scenarios.forEach { (id, label) ->
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    isStressLoading = true
                                    val tickers = if (meshHoldings.isNotEmpty()) meshHoldings.keys.toList() else listOf("TCS", "RELIANCE")
                                    val weights = if (meshHoldings.isNotEmpty()) meshHoldings.values.toList() else listOf(0.5, 0.5)
                                    stressTestResult = repository.runStressTest(tickers, weights, id)
                                    isStressLoading = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isStressLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                }

                stressTestResult?.let { res ->
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SCENARIO: ${res.scenarioName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        BentoMetric(label = "Return Shock", value = "${String.format("%.1f", res.portfolioReturnChange * 100)}%", modifier = Modifier.weight(1f))
                        BentoMetric(label = "Stressed VaR", value = "${String.format("%.1f", res.stressedVaR * 100)}%", modifier = Modifier.weight(1f))
                        BentoMetric(label = "Max Drawdown", value = "${String.format("%.1f", res.stressedDrawdown * 100)}%", modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "💡 FINNY Verdict: ${res.plainEnglishVerdict}",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item(key = "config_card") {
            DoubleBezelCard {
                MetadataLabel(text = "Asset Configuration")
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tickerInput,
                        onValueChange = { tickerInput = it },
                        label = { Text("Ticker (e.g. BTC)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Add/Update", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    PrimaryButton(
                        text = "Calculate exposure",
                        onClick = {
                            coroutineScope.launch {
                                if (meshHoldings.isEmpty()) {
                                    errorMessage = "Please add at least one asset."
                                    return@launch
                                }
                                isLoading = true
                                errorMessage = null
                                riskMeshResult = null
                                try {
                                    riskMeshResult = repository.getRiskMesh(meshHoldings.keys.toList(), meshHoldings.values.toList())
                                } catch (e: Exception) {
                                    errorMessage = "Risk engine connection failure: ${e.message ?: "Unknown error"}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        isLoading = isLoading
                    )
                }

                if (meshHoldings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MetadataLabel(text = "Current Basket: ${meshHoldings.keys.joinToString(", ")}")
                }
            }
        }

        errorMessage?.let { error ->
            item(key = "error") {
                StatusMessage(error, isError = true)
            }
        }

        riskMeshResult?.let { res ->
            item {
                DoubleBezelCard {
                    MetadataLabel(text = "Exposure Summary")
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            MetadataLabel(text = "Net Exposure Index")
                            Text(text = "${res.netExposureIndex}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(getNEIDesc(res.netExposureIndex), fontSize = 10.sp, color = getNEIColor(res.netExposureIndex), fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            MetadataLabel(text = "Portfolio Beta")
                            Text(text = "${res.portfolioBeta}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(getBetaDesc(res.portfolioBeta), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }

                    res.aiRiskAudit?.let { audit ->
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        MetadataLabel(text = "RISK RESPONSE", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = audit,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            item {
                DoubleBezelCard {
                    MetadataLabel(text = "Asset Correlation Heatmap")
                    Spacer(modifier = Modifier.height(24.dp))
                    CorrelationHeatmap(res.tickers, res.correlationMatrix)
                }
            }

            if (res.redundantExposures.isNotEmpty()) {
                item {
                    Column {
                        MetadataLabel(text = "Concentration Alerts")
                        Spacer(modifier = Modifier.height(16.dp))
                        res.redundantExposures.forEach { alert ->
                            RiskWarningCard(alert.warning, Color(0xFF9F2F2D), Color(0xFFFDEBEC))
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RiskWarningCard(text: String, textColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(text = text, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CorrelationHeatmap(tickers: List<String>, matrix: Map<String, Map<String, Double>>) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f))
            tickers.forEach { ticker ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(ticker, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        tickers.forEach { rowTicker ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Text(rowTicker, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                tickers.forEach { colTicker ->
                    val corr = matrix[rowTicker]?.get(colTicker) ?: 1.0
                    val cellColor = getHeatmapColor(corr)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(1.dp)
                            .background(cellColor, shape = RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%.2f", corr),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (corr.let { it > 0.6 || it < -0.2 }) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    }
}

private fun getHeatmapColor(corr: Double): Color {
    return when {
        corr >= 0.8 -> Color(0xFF111111)
        corr >= 0.5 -> Color(0xFF787774)
        corr >= 0.2 -> Color(0xFFEAEAEA)
        corr <= -0.5 -> Color(0xFF9F2F2D)
        corr <= -0.1 -> Color(0xFFFDEBEC)
        else -> Color(0xFFFBFBFA)
    }
}

private fun getNEIColor(nei: Double): Color = if (nei > 0.5) Color(0xFF9F2F2D) else Color(0xFF346538)
private fun getNEIDesc(nei: Double): String = if (nei > 0.5) "CONCENTRATED RISK" else "OPTIMALLY DIVERSIFIED"
private fun getBetaDesc(beta: Double): String = if (beta > 1.2) "HIGH VOLATILITY" else "MARKET NEUTRAL"

@Preview(showBackground = true)
@Composable
fun RiskMeshScreenPreview() {
    PrototyxTheme {
        RiskMeshScreen(repository = MockDataRepository())
    }
}

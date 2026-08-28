package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.data.model.RiskMeshResponse
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun RiskMeshScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    // State for Dynamic Holdings
    var holdings by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var tickerInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }

    var riskMeshResult by remember { mutableStateOf<RiskMeshResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item(key = "heading") {
            Column {
                EditorialHeading(text = "Cross-Asset Risk Mesh")
                Spacer(modifier = Modifier.height(8.dp))
                MetadataLabel(text = "Systemic Exposure & Correlation Analysis")
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
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.1f)
                        )
                    )
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.1f)
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
                                val newHoldings = holdings.toMutableMap()
                                if (weight > 0) newHoldings[ticker] = weight else newHoldings.remove(ticker)
                                holdings = newHoldings
                                tickerInput = ""
                                weightInput = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.05f), contentColor = Color.Black)
                    ) {
                        Text("Add/Update", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    PrimaryButton(
                        text = "Analyze Mesh",
                        onClick = {
                            coroutineScope.launch {
                                if (holdings.isEmpty()) {
                                    errorMessage = "Please add at least one asset."
                                    return@launch
                                }
                                isLoading = true
                                errorMessage = null
                                riskMeshResult = null
                                try {
                                    riskMeshResult = repository.getRiskMesh(holdings.keys.toList(), holdings.values.toList())
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
                
                if (holdings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MetadataLabel(text = "Current Basket: ${holdings.keys.joinToString(", ")}")
                }
            }
        }

        errorMessage?.let { error ->
            item(key = "error") {
                Text(text = error, color = Color.Red, fontSize = 12.sp)
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
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(24.dp))
                        MetadataLabel(text = "Defense Strategy", color = Color(0xFF9F2F2D))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = audit,
                            fontSize = 13.sp,
                            color = Color.Black.copy(alpha = 0.7f),
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

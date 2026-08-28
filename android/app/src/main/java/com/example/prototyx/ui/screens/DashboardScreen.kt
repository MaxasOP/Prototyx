package com.example.prototyx.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.MarketIndicatorsResponse
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.*

@Composable
fun DashboardScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    // State for Tickers and their Weights (Portfolio Holdings)
    var holdings by remember { mutableStateOf(mapOf("TCS" to 0.30f, "RELIANCE" to 0.40f, "AAPL" to 0.20f, "INFY" to 0.10f)) }
    var selectedTicker by remember { mutableStateOf("TCS") }
    var tickerData by remember { mutableStateOf<MarketIndicatorsResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var searchWeight by remember { mutableStateOf("") }

    val activeTickers = remember(holdings) { holdings.keys.toList() }
    val currentWeights = remember(holdings) { holdings.values.toList() }
    
    val defaultColors = listOf(
        Color(0xFF111111), Color(0xFF787774), Color(0xFFC0C0C0), Color(0xFFE1F3FE),
        Color(0xFF1F6C9F), Color(0xFF4CAF50), Color(0xFF9C27B0), Color(0xFFFF9800)
    )

    LaunchedEffect(selectedTicker) {
        if (selectedTicker.isNotEmpty()) {
            isLoading = true
            searchError = null
            try {
                tickerData = repository.getIndicators(selectedTicker)
            } catch (e: Exception) {
                searchError = "Intelligence extraction failed for $selectedTicker: ${e.message ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Hero Header
        item(key = "heading") {
            Column {
                EditorialHeading(text = "Prototyx")
                Spacer(modifier = Modifier.height(8.dp))
                MetadataLabel(text = "DYNAMIC PORTFOLIO OPS")
            }
        }

        // Search & Discovery (Universal Input)
        item(key = "search_card") {
            DoubleBezelCard {
                MetadataLabel(text = "Asset Discovery & Allocation")
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
                    text = "Update Portfolio Mesh",
                    onClick = {
                        val ticker = searchQuery.trim().uppercase()
                        val weight = searchWeight.toDoubleOrNull()?.toFloat() ?: 0f
                        if (ticker.isNotEmpty()) {
                            val newHoldings = holdings.toMutableMap()
                            if (weight > 0) {
                                newHoldings[ticker] = weight
                            } else {
                                newHoldings.remove(ticker)
                            }
                            // Re-normalize weights to sum to 1.0 if needed, or just keep as is
                            holdings = newHoldings
                            selectedTicker = ticker
                            searchQuery = ""
                            searchWeight = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Enter weight 0 to remove an asset.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Real-time Asset Intelligence
        item(key = "asset_intel") {
            DoubleBezelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorialHeading(text = "Asset Intelligence", modifier = Modifier.weight(1f))
                    
                    if (activeTickers.isNotEmpty()) {
                        TextButton(onClick = {
                            val nextIndex = (activeTickers.indexOf(selectedTicker) + 1) % activeTickers.size
                            selectedTicker = activeTickers[nextIndex]
                        }) {
                            MetadataLabel(text = "ACTIVE: $selectedTicker", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    }
                } else if (searchError != null) {
                    Text(text = searchError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                } else if (tickerData != null) tickerData?.let { data ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        BentoMetric(label = "Last Price", value = "₹${data.close}", modifier = Modifier.weight(1f))
                        BentoMetric(label = "PE Ratio", value = "${data.peRatio ?: "N/A"}", modifier = Modifier.weight(1f))
                        BentoMetric(label = "Div Yield", value = "${String.format("%.2f", (data.dividendYield ?: 0.0) * 100)}%", modifier = Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    MetadataLabel(text = "Technical Mesh Metrics")
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TechnicalMetricItem("Relative Strength Index", data.rsi?.let { String.format("%.2f", it) } ?: "N/A")
                    TechnicalMetricItem("50-Day Moving Avg", "₹${data.sma50?.let { String.format("%.2f", it) } ?: "N/A"}")
                    TechnicalMetricItem("200-Day Moving Avg", "₹${data.sma200?.let { String.format("%.2f", it) } ?: "N/A"}")

                    data.aiMemo?.let { memo ->
                        Spacer(modifier = Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = memo,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    Text("Search an asset above to load intelligence.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Multi-Asset Distribution Map (Truly Dynamic)
        if (activeTickers.isNotEmpty()) {
            item(key = "dist_map") {
                DoubleBezelCard {
                    EditorialHeading(text = "Portfolio Exposure")
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        PortfolioPieChart(
                            weights = currentWeights, 
                            colors = defaultColors.take(activeTickers.size).let { 
                                if (it.size < activeTickers.size) it + List(activeTickers.size - it.size) { Color.Gray } else it 
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    val totalWeight = currentWeights.sum()
                    val legendItems = activeTickers.mapIndexed { index, ticker ->
                        val weight = holdings[ticker] ?: 0f
                        val percentage = if (totalWeight > 0) (weight / totalWeight * 100).toInt() else 0
                        val color = if (index < defaultColors.size) defaultColors[index] else Color.Gray
                        "$ticker ($percentage%)" to color
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        legendItems.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowItems.forEach { (label, color) ->
                                    Box(modifier = Modifier.weight(1f)) {
                                        LegendItem(label, color)
                                    }
                                }
                                if (rowItems.size == 1) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
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
fun PortfolioPieChart(weights: List<Float>, colors: List<Color>) {
    Canvas(
        modifier = Modifier
            .size(180.dp)
    ) {
        var startAngle = -90f
        val sum = weights.sum()
        for (i in weights.indices) {
            val sweepAngle = (weights[i] / sum) * 360f
            drawArc(
                color = colors[i],
                startAngle = startAngle,
                sweepAngle = sweepAngle - 2f, // Soft separation
                useCenter = false,
                style = Stroke(width = 12.dp.toPx()) // Thinner stroke
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    PrototyxTheme {
        DashboardScreen(repository = MockDataRepository())
    }
}

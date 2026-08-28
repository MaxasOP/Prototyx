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
    var activeTickers by remember { mutableStateOf(listOf("TCS", "RELIANCE", "AAPL", "INFY")) }
    var selectedTicker by remember { mutableStateOf("TCS") }
    var tickerData by remember { mutableStateOf<MarketIndicatorsResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    val defaultWeights = listOf(30f, 40f, 20f, 10f)
    val defaultColors = listOf(Color(0xFF111111), Color(0xFF787774), Color(0xFFEAEAEA), Color(0xFFE1F3FE))

    LaunchedEffect(selectedTicker) {
        isLoading = true
        searchError = null
        try {
            tickerData = repository.getIndicators(selectedTicker)
        } catch (e: Exception) {
            searchError = "Terminal connection failed. Verify asset mesh server status."
        } finally {
            isLoading = false
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
        item {
            Column {
                EditorialHeading(text = "Wealth Intel & Portfolio Ops")
                Spacer(modifier = Modifier.height(8.dp))
                MetadataLabel(text = "Active Portfolio Tracking & Intelligence")
            }
        }

        // Real-time Asset Intelligence
        item {
            DoubleBezelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorialHeading(text = "Asset Intelligence", modifier = Modifier.weight(1f))
                    
                    TextButton(onClick = {
                        val nextIndex = (activeTickers.indexOf(selectedTicker) + 1) % activeTickers.size
                        selectedTicker = activeTickers[nextIndex]
                    }) {
                        MetadataLabel(text = "Active: $selectedTicker", color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                } else if (searchError != null) {
                    Text(text = searchError ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                } else tickerData?.let { data ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        BentoMetric(label = "Last Price", value = "₹${data.close}", modifier = Modifier.weight(1f))
                        BentoMetric(label = "PE Ratio", value = "${data.peRatio ?: "N/A"}", modifier = Modifier.weight(1f))
                        BentoMetric(label = "Div Yield", value = "${((data.dividendYield ?: 0.0) * 100)}%", modifier = Modifier.weight(1f))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    MetadataLabel(text = "Technical Mesh Metrics")
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TechnicalMetricItem("Relative Strength Index", data.rsi?.let { String.format("%.2f", it) } ?: "N/A")
                    TechnicalMetricItem("50-Day Moving Avg", "₹${data.sma50?.let { String.format("%.2f", it) } ?: "N/A"}")
                    TechnicalMetricItem("200-Day Moving Avg", "₹${data.sma200?.let { String.format("%.2f", it) } ?: "N/A"}")
                }
            }
        }

        // Multi-Asset Distribution Map
        item {
            DoubleBezelCard {
                EditorialHeading(text = "Distribution Map")
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PortfolioPieChart(weights = defaultWeights, colors = defaultColors)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                val legendItems = listOf(
                    "TCS (30%)" to Color(0xFF111111),
                    "RELIANCE (40%)" to Color(0xFF787774),
                    "AAPL (20%)" to Color(0xFFEAEAEA),
                    "INFY (10%)" to Color(0xFFE1F3FE)
                )

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

@Composable
fun TechnicalMetricItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    PrototyxTheme {
        DashboardScreen(repository = MockDataRepository())
    }
}

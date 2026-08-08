package com.example.prototyx.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var activeTickers by remember { mutableStateOf(listOf("TCS", "RELIANCE", "AAPL", "INFY")) }
    var selectedTicker by remember { mutableStateOf("TCS") }
    var tickerData by remember { mutableStateOf<MarketIndicatorsResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // Fetch details for the default selected ticker
    LaunchedEffect(selectedTicker) {
        isLoading = true
        searchError = null
        try {
            tickerData = repository.getIndicators(selectedTicker)
        } catch (e: Exception) {
            searchError = "Could not reach Prototyx backend server. Verify server is running on http://localhost:8000."
        } finally {
            isLoading = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Prototyx Advisor Desk",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "JM Financial Pitch Demo App • Active Portfolio Tracking",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Selected Ticker Analytics Summary
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Market Ingestion Overview",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // Simple dropdown selector
                        TextButton(onClick = {
                            val nextIndex = (activeTickers.indexOf(selectedTicker) + 1) % activeTickers.size
                            selectedTicker = activeTickers[nextIndex]
                        }) {
                            Text("Switch Stock: $selectedTicker")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (searchError != null) {
                        Text(text = searchError ?: "", color = Color.Red, fontSize = 14.sp)
                    } else tickerData?.let { data ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Last Price", fontSize = 12.sp, color = Color.Gray)
                                Text("₹${data.close}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("PE Ratio", fontSize = 12.sp, color = Color.Gray)
                                Text("${data.peRatio ?: "N/A"}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Div Yield", fontSize = 12.sp, color = Color.Gray)
                                Text("${((data.dividendYield ?: 0.0) * 100)}%", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(text = "Technical Metrics (via shashankvemuri/Finance)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "• Relative Strength Index (RSI): ${data.rsi?.let { String.format("%.2f", it) } ?: "N/A"}", fontSize = 14.sp)
                        Text(text = "• 50-day Moving Avg (SMA): ₹${data.sma50?.let { String.format("%.2f", it) } ?: "N/A"}", fontSize = 14.sp)
                        Text(text = "• 200-day Moving Avg (SMA): ₹${data.sma200?.let { String.format("%.2f", it) } ?: "N/A"}", fontSize = 14.sp)
                    }
                }
            }
        }

        // Canvas Pie Chart (Demonstrates Custom Drawing in Jetpack Compose)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Ghostfolio-Inspired Asset Allocation",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    PortfolioPieChart()
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        LegendItem("TCS (30%)", Color(0xFF4CAF50))
                        LegendItem("RELIANCE (40%)", Color(0xFF2196F3))
                        LegendItem("AAPL (20%)", Color(0xFFFF9800))
                        LegendItem("INFY (10%)", Color(0xFF9C27B0))
                    }
                }
            }
        }
    }
}

@Composable
fun PortfolioPieChart() {
    val colors = listOf(Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF9C27B0))
    val weights = listOf(30f, 40f, 20f, 10f)

    Canvas(
        modifier = Modifier
            .size(160.dp)
    ) {
        var startAngle = 0f
        for (i in weights.indices) {
            val sweepAngle = (weights[i] / 100f) * 360f
            drawArc(
                color = colors[i],
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 30.dp.toPx())
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
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    PrototyxTheme {
        DashboardScreen(repository = MockDataRepository())
    }
}

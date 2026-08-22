package com.example.prototyx.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.MarketIndicatorsResponse
import com.example.prototyx.data.model.PaytmAsset
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    var activeTickers by remember { mutableStateOf(listOf("TCS", "RELIANCE", "AAPL", "INFY")) }
    var selectedTicker by remember { mutableStateOf("TCS") }
    var tickerData by remember { mutableStateOf<MarketIndicatorsResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    // Paytm Session States
    var paytmConnected by remember { mutableStateOf(false) }
    var paytmSource by remember { mutableStateOf("Not Connected") }
    var paytmHoldings by remember { mutableStateOf<List<PaytmAsset>>(emptyList()) }
    var paytmLoading by remember { mutableStateOf(false) }

    val defaultWeights = listOf(30f, 40f, 20f, 10f)
    val defaultColors = listOf(Color(0xFF111111), Color(0xFF787774), Color(0xFFEAEAEA), Color(0xFFE1F3FE))
    val colorsPalette = listOf(Color(0xFF111111), Color(0xFF787774), Color(0xFFB0BEC5), Color(0xFFEAEAEA), Color(0xFFE1F3FE), Color(0xFFFFCC80))

    // Fetch Paytm holdings function
    val refreshPaytm = {
        paytmLoading = true
        coroutineScope.launch {
            try {
                val res = repository.getPaytmHoldings()
                paytmConnected = res.connected
                paytmSource = res.source
                paytmHoldings = res.holdings
                if (res.holdings.isNotEmpty()) {
                    activeTickers = res.holdings.map { it.ticker }
                    if (!activeTickers.contains(selectedTicker)) {
                        selectedTicker = activeTickers.first()
                    }
                }
            } catch (e: Exception) {
                // Server offline
            } finally {
                paytmLoading = false
            }
        }
    }

    // Initial load
    LaunchedEffect(Unit) {
        refreshPaytm()
    }

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

        // Paytm Money OAuth Integration Card
        item {
            DoubleBezelCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorialHeading(text = "Broker Connection Desk", modifier = Modifier.weight(1f))
                    
                    Box(
                        modifier = Modifier
                            .background(
                                if (paytmConnected) Color(0xFFE8F5E9) else Color(0xFFECEFF1),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (paytmConnected) "Paytm Linked" else "Disconnected",
                            color = if (paytmConnected) Color(0xFF2E7D32) else Color(0xFF546E7A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (paytmLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (paytmConnected) "Source: $paytmSource. Live holdings imported securely from Paytm Money API." 
                               else "Link Paytm Money broker portfolio securely. Secrets are stored strictly on backend server.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!paytmConnected) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val res = repository.getPaytmLoginUrl()
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(res.url))
                                            context.startActivity(browserIntent)
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Connect Paytm")
                            }
                            
                            OutlinedButton(
                                onClick = { refreshPaytm() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Refresh Holdings")
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            repository.disconnectPaytm()
                                            paytmConnected = false
                                            paytmHoldings = emptyList()
                                            activeTickers = listOf("TCS", "RELIANCE", "AAPL", "INFY")
                                            selectedTicker = "TCS"
                                        } catch (e: Exception) {
                                            // Handle error
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Disconnect Broker")
                            }
                        }
                    }
                }
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
                
                val chartWeights = if (paytmConnected && paytmHoldings.isNotEmpty()) {
                    paytmHoldings.map { (it.weight * 100).toFloat() }
                } else {
                    defaultWeights
                }
                
                val chartColors = if (paytmConnected && paytmHoldings.isNotEmpty()) {
                    paytmHoldings.indices.map { colorsPalette[it % colorsPalette.size] }
                } else {
                    defaultColors
                }

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PortfolioPieChart(weights = chartWeights, colors = chartColors)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Legend list
                val legendItems = if (paytmConnected && paytmHoldings.isNotEmpty()) {
                    paytmHoldings.mapIndexed { idx, asset ->
                        "${asset.ticker} (${String.format("%.0f", asset.weight * 100)}%)" to colorsPalette[idx % colorsPalette.size]
                    }
                } else {
                    listOf(
                        "TCS (30%)" to Color(0xFF111111),
                        "RELIANCE (40%)" to Color(0xFF787774),
                        "AAPL (20%)" to Color(0xFFEAEAEA),
                        "INFY (10%)" to Color(0xFFE1F3FE)
                    )
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

package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import kotlinx.coroutines.launch

@Composable
fun RiskMeshScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var inputHoldings by remember { mutableStateOf("TCS=0.30, RELIANCE=0.40, AAPL=0.20, INFY=0.10") }
    
    var riskMeshResult by remember { mutableStateOf<RiskMeshResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Cross-Asset Correlation Mesh",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Calculates asset-to-asset correlations & portfolio net exposure index to catch hidden portfolio overlaps.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputHoldings,
                        onValueChange = { inputHoldings = it },
                        label = { Text("Portfolio Holdings (e.g. TCS=0.30, AAPL=0.20)") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Specify tickers and decimal weights summing to 1") }
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                riskMeshResult = null
                                try {
                                    val tickers = mutableListOf<String>()
                                    val weights = mutableListOf<Double>()
                                    
                                    inputHoldings.split(",").forEach { item ->
                                        val parts = item.split("=")
                                        if (parts.size == 2) {
                                            val t = parts[0].trim().uppercase()
                                            val w = parts[1].trim().toDoubleOrNull()
                                            if (w != null && t.isNotEmpty()) {
                                                tickers.add(t)
                                                weights.add(w)
                                            }
                                        }
                                    }

                                    riskMeshResult = repository.getRiskMesh(tickers, weights)
                                } catch (e: Exception) {
                                    errorMessage = "Failed to calculate correlation mesh. Verify backend connectivity."
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Compute Risk & Correlation Mesh")
                        }
                    }
                }
            }
        }

        errorMessage?.let { error ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        riskMeshResult?.let { res ->
            // Net Exposure & Beta Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Net Exposure Index", fontSize = 11.sp, color = Color.Gray)
                            Text("${res.netExposureIndex}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = getNEIColor(res.netExposureIndex))
                            Text(getNEIDesc(res.netExposureIndex), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Portfolio Beta (v/s Nifty)", fontSize = 11.sp, color = Color.Gray)
                            Text("${res.portfolioBeta}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(getBetaDesc(res.portfolioBeta), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Correlation Matrix Grid
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Cross-Asset Heatmap Matrix",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        CorrelationHeatmap(res.tickers, res.correlationMatrix)
                    }
                }
            }

            // Warning Alerts List
            if (res.redundantExposures.isNotEmpty()) {
                item {
                    Text(text = "Exposures Warnings", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                items(res.redundantExposures) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.warning,
                            fontSize = 13.sp,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Hedging list
            if (res.hedgedPositions.isNotEmpty()) {
                item {
                    Text(text = "Hedged Buffers", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                items(res.hedgedPositions) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = item.details,
                            fontSize = 13.sp,
                            color = Color(0xFF1B5E20),
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RiskMeshScreenPreview() {
    PrototyxTheme {
        RiskMeshScreen(repository = MockDataRepository())
    }
}

@Composable
fun CorrelationHeatmap(tickers: List<String>, matrix: Map<String, Map<String, Double>>) {
    Column {
        // Ticker column headers
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f)) // Empty top-left cell
            tickers.forEach { ticker ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(ticker, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        tickers.forEach { rowTicker ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Row header
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(rowTicker, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (corr > 0.6 || corr < -0.2) Color.White else Color.Black
                        )
                    }
                }
            }
        }
    }
}

private fun getHeatmapColor(valStr: Double): Color {
    return when {
        valStr >= 0.8 -> Color(0xFF1E88E5) // deep blue - high positive correlation
        valStr >= 0.5 -> Color(0xFF64B5F6) // light blue
        valStr >= 0.2 -> Color(0xFFBBDEFB) // pale blue
        valStr <= -0.5 -> Color(0xFFE53935) // deep red - high negative correlation
        valStr <= -0.1 -> Color(0xFFEF9A9A) // light red
        else -> Color(0xFFEEEEEE) // grey - uncorrelated
    }
}

private fun getNEIColor(nei: Double): Color {
    return when {
        nei > 0.6 -> Color(0xFFE53935) // high concentration - red
        nei > 0.3 -> Color(0xFFFF9800) // moderate - orange
        else -> Color(0xFF4CAF50) // diversified - green
    }
}

private fun getNEIDesc(nei: Double): String {
    return when {
        nei > 0.6 -> "High Overlap Risk"
        nei > 0.3 -> "Moderate Risk"
        else -> "Diversified Basket"
    }
}

private fun getBetaDesc(beta: Double): String {
    return when {
        beta > 1.2 -> "High Volatility"
        beta < 0.8 -> "Defensive Basket"
        else -> "Market Neutral"
    }
}

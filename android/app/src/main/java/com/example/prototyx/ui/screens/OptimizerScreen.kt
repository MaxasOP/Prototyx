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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.OptimizeResponse
import com.example.prototyx.theme.PrototyxTheme
import kotlinx.coroutines.launch

@Composable
fun OptimizerScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var tickerInput by remember { mutableStateOf("TCS, RELIANCE, AAPL, INFY") }
    var viewsInput by remember { mutableStateOf("TCS=0.16, RELIANCE=0.14, AAPL=0.18") }
    
    var optimizationResult by remember { mutableStateOf<OptimizeResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explainer Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Black-Litterman Portfolio Optimizer",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Blends historical market covariance (via PyPortfolioOpt) with dynamic views to output robust asset allocations.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Inputs Card
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
                        value = tickerInput,
                        onValueChange = { tickerInput = it },
                        label = { Text("Asset Tickers (comma separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = viewsInput,
                        onValueChange = { viewsInput = it },
                        label = { Text("Subjective Views (e.g. TCS=0.15, AAPL=0.18)") },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text("Implied annual return forecasts from analysts") }
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                optimizationResult = null
                                try {
                                    val tickers = tickerInput.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                                    
                                    // Parse views mapping
                                    val views = mutableMapOf<String, Double>()
                                    if (viewsInput.isNotEmpty()) {
                                        viewsInput.split(",").forEach { item ->
                                            val parts = item.split("=")
                                            if (parts.size == 2) {
                                                val ticker = parts[0].trim().uppercase()
                                                val valStr = parts[1].trim()
                                                val value = valStr.toDoubleOrNull()
                                                if (value != null) {
                                                    views[ticker] = value
                                                }
                                            }
                                        }
                                    }

                                    optimizationResult = repository.getOptimization(tickers, if (views.isNotEmpty()) views else null)
                                } catch (e: Exception) {
                                    errorMessage = "Optimization failed. Check if local Python server is running."
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
                            Text("Solve Black-Litterman Weights")
                        }
                    }
                }
            }
        }

        // Error message if any
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

        // Optimization output results
        optimizationResult?.let { res ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Model Outputs (Solver: ${res.method})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Expected Return", fontSize = 11.sp, color = Color.Gray)
                                Text("${String.format("%.2f", res.expectedAnnualReturn * 100)}%", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Volatility Risk", fontSize = 11.sp, color = Color.Gray)
                                Text("${String.format("%.2f", res.annualVolatility * 100)}%", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Sharpe Ratio", fontSize = 11.sp, color = Color.Gray)
                                Text(String.format("%.2f", res.sharpeRatio), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Optimal Allocations:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        res.weights.forEach { (ticker, weight) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.bind()),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = ticker, fontWeight = FontWeight.SemiBold)
                                Text(text = "${String.format("%.2f", weight * 100)}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            // Custom horizontal bar to visualize weight
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(Color.LightGray, shape = RoundedCornerShape(4.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(weight.toFloat().coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// Simple extension helper for Int padding.dp binding
private fun Int.bind() = this.dp

@Preview(showBackground = true)
@Composable
fun OptimizerScreenPreview() {
    PrototyxTheme {
        OptimizerScreen(repository = MockDataRepository())
    }
}

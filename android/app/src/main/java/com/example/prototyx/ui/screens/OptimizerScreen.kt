package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.prototyx.ui.components.*
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

    // Load mock data if null (for previews and initial state)
    LaunchedEffect(Unit) {
        if (optimizationResult == null) {
            try {
                optimizationResult = repository.getOptimization(
                    tickerInput.split(",").map { it.trim() },
                    mapOf("TCS" to 0.16)
                )
            } catch (e: Exception) {
                // Silently fail for initial load
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
        // Header
        item {
            Column {
                EditorialHeading(text = "Black-Litterman Strategic Optimization")
                Spacer(modifier = Modifier.height(8.dp))
                MetadataLabel(text = "Advanced Bayesian Portfolio Balancing")
            }
        }

        // Configuration
        item {
            DoubleBezelCard {
                MetadataLabel(text = "Optimization Parameters")
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = tickerInput,
                    onValueChange = { tickerInput = it },
                    label = { Text("Asset Universe", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = viewsInput,
                    onValueChange = { viewsInput = it },
                    label = { Text("Subjective Return Views", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Solve Strategic Weights",
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            optimizationResult = null
                            try {
                                val tickers = tickerInput.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                                val views = mutableMapOf<String, Double>()
                                if (viewsInput.isNotEmpty()) {
                                    viewsInput.split(",").forEach { item ->
                                        val parts = item.split("=")
                                        if (parts.size == 2) {
                                            val ticker = parts[0].trim().uppercase()
                                            val value = parts[1].trim().toDoubleOrNull()
                                            if (value != null) views[ticker] = value
                                        }
                                    }
                                }
                                optimizationResult = repository.getOptimization(tickers, if (views.isNotEmpty()) views else null)
                            } catch (e: Exception) {
                                errorMessage = "Solver engine unreachable. Verify Python backend."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoading
                )
            }
        }

        errorMessage?.let { error ->
            item {
                Text(text = error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }

        // Results
        optimizationResult?.let { res ->
            item {
                DoubleBezelCard {
                    MetadataLabel(text = "Engine Output: ${res.method}")
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        BentoMetric(label = "Expected Return", value = "${String.format("%.2f", res.expectedAnnualReturn * 100)}%", modifier = Modifier.weight(1f))
                        BentoMetric(label = "Volatility Risk", value = "${String.format("%.2f", res.annualVolatility * 100)}%", modifier = Modifier.weight(1f))
                        BentoMetric(label = "Sharpe Ratio", value = String.format("%.2f", res.sharpeRatio), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    MetadataLabel(text = "Target Allocations")
                    Spacer(modifier = Modifier.height(16.dp))

                    res.weights.forEach { (ticker, weight) ->
                        AllocationRow(ticker, weight)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AllocationRow(ticker: String, weight: Double) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = ticker, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(text = "${String.format("%.2f", weight * 100)}%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.Black.copy(alpha = 0.05f), shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(weight.toFloat().coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(Color.Black, shape = androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OptimizerScreenPreview() {
    PrototyxTheme {
        OptimizerScreen(repository = MockDataRepository())
    }
}

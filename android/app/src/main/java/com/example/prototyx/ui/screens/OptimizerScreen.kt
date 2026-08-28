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
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun OptimizerScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    
    // State for Dynamic Universe and Views
    var universe by remember { mutableStateOf<List<String>>(emptyList()) }
    var views by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    var tickerInput by remember { mutableStateOf("") }
    var viewInput by remember { mutableStateOf("") }
    
    var optimizationResult by remember { mutableStateOf<OptimizeResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Header
        item(key = "heading") {
            Column {
                EditorialHeading(text = "Black-Litterman Strategic Optimization")
                Spacer(modifier = Modifier.height(8.dp))
                MetadataLabel(text = "Advanced Bayesian Portfolio Balancing")
            }
        }

        // Configuration (Universal Discovery)
        item(key = "config_card") {
            DoubleBezelCard {
                MetadataLabel(text = "Universe & Forecasts")
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tickerInput,
                        onValueChange = { tickerInput = it },
                        label = { Text("Ticker", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.1f)
                        )
                    )
                    OutlinedTextField(
                        value = viewInput,
                        onValueChange = { viewInput = it },
                        label = { Text("Exp. Return", fontSize = 11.sp) },
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
                            val expectedReturn = viewInput.toDoubleOrNull()
                            if (ticker.isNotEmpty()) {
                                if (!universe.contains(ticker)) universe = universe + ticker
                                if (expectedReturn != null) {
                                    views = views + (ticker to expectedReturn)
                                }
                                tickerInput = ""
                                viewInput = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.05f), contentColor = Color.Black)
                    ) {
                        Text("Add Asset", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    PrimaryButton(
                        text = "Solve Weights",
                        onClick = {
                            coroutineScope.launch {
                                if (universe.isEmpty()) {
                                    errorMessage = "Please add assets to your universe."
                                    return@launch
                                }
                                isLoading = true
                                errorMessage = null
                                optimizationResult = null
                                try {
                                    optimizationResult = repository.getOptimization(universe, if (views.isNotEmpty()) views else null)
                                } catch (e: Exception) {
                                    errorMessage = "Solver engine unreachable: ${e.message ?: "Verify Python backend"}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        isLoading = isLoading
                    )
                }

                if (universe.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    MetadataLabel(text = "Universe: ${universe.joinToString(", ")}")
                    if (views.isNotEmpty()) {
                        Text(
                            text = "Views: ${views.map { "${it.key}=${(it.value*100).toInt()}%" }.joinToString(", ")}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
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

                    res.aiRationale?.let { rationale ->
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                        Spacer(modifier = Modifier.height(24.dp))
                        MetadataLabel(text = "Optimization Rationale", color = Color(0xFF1F6C9F))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = rationale,
                            fontSize = 13.sp,
                            color = Color.Black.copy(alpha = 0.7f),
                            lineHeight = 20.sp
                        )
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

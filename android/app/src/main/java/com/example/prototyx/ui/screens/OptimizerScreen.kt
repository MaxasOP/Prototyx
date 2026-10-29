package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    modifier: Modifier = Modifier,
    onPortfolio: () -> Unit = {},
    onRisk: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    
    // State for Portfolio (Source of Truth)
    val portfolioHoldings by repository.holdings.collectAsState(initial = emptyMap())

    // State for Dynamic Universe and Views
    var universe by remember { mutableStateOf<List<String>>(emptyList()) }
    var views by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    
    var tickerInput by remember { mutableStateOf("") }
    var viewInput by remember { mutableStateOf("") }
    
    var optimizationResult by remember { mutableStateOf<OptimizeResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var confirmApply by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    if (confirmApply) AlertDialog(onDismissRequest = { if (!isSaving) confirmApply = false },
        title = { Text("Replace portfolio allocation?") },
        text = { Text("This saves the target weights shown below and removes holdings outside this target. It does not place trades.") },
        confirmButton = { TextButton(enabled = !isSaving, onClick = {
            coroutineScope.launch {
                isSaving = true
                try {
                    optimizationResult?.let { repository.replaceHoldings(it.weights.mapValues { weight -> weight.value.toFloat() }) }
                    confirmApply = false; saved = true; errorMessage = null
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { confirmApply = false; errorMessage = "Could not save allocation: ${e.message}" }
                finally { isSaving = false }
            }
        }) { Text(if (isSaving) "Saving…" else "Replace allocation") } },
        dismissButton = { TextButton(enabled = !isSaving, onClick = { confirmApply = false }) { Text("Keep portfolio") } })

    // Sync universe with portfolio on first load
    LaunchedEffect(portfolioHoldings) {
        if (universe.isEmpty() && portfolioHoldings.isNotEmpty()) {
            universe = portfolioHoldings.keys.toList()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // Header
        item(key = "heading") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    EditorialHeading(text = "Rebalance")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Compare your current weights with a calculated target, then decide whether to save it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Configuration (Universal Discovery)
        item(key = "config_card") {
            DoubleBezelCard {
                MetadataLabel(text = "INVESTMENT UNIVERSE")
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tickerInput,
                        onValueChange = { tickerInput = it },
                        label = { Text("Ticker", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = viewInput,
                        onValueChange = { viewInput = it },
                        label = { Text("Return % (optional)", fontSize = 11.sp) },
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
                            val expectedReturn = viewInput.toDoubleOrNull()
                            if (viewInput.isNotBlank() && (expectedReturn == null || !expectedReturn.isFinite() || expectedReturn !in -100.0..100.0)) {
                                errorMessage = "Enter an expected return between -100% and 100%, or leave it blank."
                                return@Button
                            }
                            if (ticker.isNotEmpty()) {
                                if (!universe.contains(ticker)) universe = universe + ticker
                                if (expectedReturn != null) {
                                    views = views + (ticker to expectedReturn / 100.0)
                                }
                                tickerInput = ""
                                viewInput = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Text("Add Asset", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    PrimaryButton(
                        text = "Calculate target",
                        onClick = {
                            coroutineScope.launch {
                                if (universe.isEmpty()) {
                                    errorMessage = "Please add assets to your universe."
                                    return@launch
                                }
                                isLoading = true
                                saved = false
                                errorMessage = null
                                optimizationResult = null
                                try {
                                    optimizationResult = repository.getOptimization(universe, if (views.isNotEmpty()) views else null)
                                } catch (e: Exception) {
                                    errorMessage = "Could not calculate a target. Check the assets and try again."
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
                    TextButton(onClick = { universe = emptyList(); views = emptyMap(); optimizationResult = null; saved = false }) { Text("Clear assets and views") }
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
                    MetadataLabel(text = res.method)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        BentoMetric(label = "Expected Return", value = "${String.format("%.2f", res.expectedAnnualReturn * 100)}%", modifier = Modifier.weight(1f))
                        BentoMetric(label = "Volatility Risk", value = "${String.format("%.2f", res.annualVolatility * 100)}%", modifier = Modifier.weight(1f))
                        BentoMetric(label = "Sharpe Ratio", value = String.format("%.2f", res.sharpeRatio), modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    MetadataLabel(text = "Target Allocations")
                    Spacer(modifier = Modifier.height(16.dp))

                    res.weights.forEach { (ticker, weight) ->
                        AllocationRow(ticker, weight)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(20.dp))

                    MetadataLabel(text = "REBALANCE")
                    Text("Changes below are percentage points of the whole portfolio, including any unallocated share.", style = MaterialTheme.typography.bodySmall)
                    PrimaryButton(text = "Apply target allocation", onClick = { confirmApply = true }, modifier = Modifier.fillMaxWidth())
                    if (saved) {
                        Text("Target allocation saved.", style = MaterialTheme.typography.bodyMedium)
                        Row {
                            TextButton(onClick = onPortfolio) { Text("View portfolio") }
                            TextButton(onClick = onRisk) { Text("Check updated risk") }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    (res.weights.keys + portfolioHoldings.keys).forEach { ticker ->
                        val targetWeight = res.weights[ticker] ?: 0.0
                        val currentWeight = (portfolioHoldings[ticker] ?: 0f).toDouble()
                        val delta = targetWeight - currentWeight
                        val action = when {
                            delta > 0.02 -> "+${String.format("%.1f", delta * 100)} pp"
                            delta < -0.02 -> "${String.format("%.1f", delta * 100)} pp"
                            else -> "Within 2 pp"
                        }
                        val actionColor = when {
                            delta > 0.02 -> Color(0xFF2E7D32)
                            delta < -0.02 -> Color(0xFFD32F2F)
                            else -> Color.Gray
                        }

                        RebalanceActionRow(
                            ticker = ticker,
                            currentWeight = currentWeight,
                            targetWeight = targetWeight,
                            action = action,
                            actionColor = actionColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    res.aiRationale?.let { rationale ->
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        MetadataLabel(text = "RATIONALE")
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = rationale,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    val animatedWeight by androidx.compose.animation.core.animateFloatAsState(
        targetValue = weight.toFloat().coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = .82f,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "allocation_$ticker"
    )
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
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedWeight)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}

@Composable
fun RebalanceActionRow(ticker: String, currentWeight: Double, targetWeight: Double, action: String, actionColor: Color) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ticker, style = MaterialTheme.typography.titleMedium)
                Text(String.format("%.1f%% → %.1f%%", currentWeight*100, targetWeight*100),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(action, style = MaterialTheme.typography.labelLarge, color = actionColor)
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

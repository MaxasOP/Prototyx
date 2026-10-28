package com.example.prototyx.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.OptimizeResponse
import com.example.prototyx.theme.Amber
import com.example.prototyx.theme.Coral
import com.example.prototyx.theme.Mint
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun OptimizerScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
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

    // Sync universe with portfolio on first load
    LaunchedEffect(portfolioHoldings) {
        if (universe.isEmpty() && portfolioHoldings.isNotEmpty()) {
            universe = portfolioHoldings.keys.toList()
        }
    }

    // Auto-solve on initial load if universe exists
    LaunchedEffect(universe) {
        if (optimizationResult == null && universe.isNotEmpty()) {
            try {
                optimizationResult = repository.getOptimization(universe, if (views.isNotEmpty()) views else null)
            } catch (e: Exception) {
                // Silently fallback to manual solver
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Terminal Header
            item(key = "header") {
                ScreenHeader(
                    category = "PROTOTYX · PORTFOLIO OPTIMIZATION",
                    title = "Portfolio Optimizer",
                    subtitle = "Solve optimal target weights using Black-Litterman and MVO models"
                )
            }

            // 2. Optimization Summary Metrics Card
            optimizationResult?.let { res ->
                item(key = "optimization_summary_card") {
                    DoubleBezelCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionHeader(title = "Target portfolio metrics")
                                StatusBadge(text = res.method.uppercase(), type = StatusBadgeType.PRIMARY)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricCard(
                                    label = "EXPECTED RETURN",
                                    value = "${String.format(Locale.US, "%.2f", res.expectedAnnualReturn * 100)}%",
                                    isPositiveChange = true,
                                    changeText = "ANNUALIZED",
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                MetricCard(
                                    label = "VOLATILITY RISK",
                                    value = "${String.format(Locale.US, "%.2f", res.annualVolatility * 100)}%",
                                    supportingText = "Std Deviation",
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                MetricCard(
                                    label = "SHARPE RATIO",
                                    value = String.format(Locale.US, "%.2f", res.sharpeRatio),
                                    supportingText = "Risk-Adjusted",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 3. Side-by-Side Allocation Comparison & Rebalance Actions
                item(key = "allocation_rebalance_section") {
                    DoubleBezelCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(
                                title = "Allocation & rebalance actions",
                                supportingText = "Comparison of current holdings vs optimal target weights"
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val totalHoldingsWeight = portfolioHoldings.values.sum().let { if (it > 0f) it else 1f }

                            res.weights.forEach { (ticker, targetWeight) ->
                                val currentWeight = ((portfolioHoldings[ticker] ?: 0f) / totalHoldingsWeight).toDouble()
                                val delta = targetWeight - currentWeight
                                val (actionText, actionType) = when {
                                    delta > 0.02 -> "+${String.format(Locale.US, "%.1f", delta * 100)}% BUY" to StatusBadgeType.POSITIVE
                                    delta < -0.02 -> "${String.format(Locale.US, "%.1f", delta * 100)}% TRIM" to StatusBadgeType.NEGATIVE
                                    else -> "HOLD" to StatusBadgeType.NEUTRAL
                                }

                                RebalanceActionCard(
                                    ticker = ticker,
                                    currentWeight = currentWeight,
                                    targetWeight = targetWeight,
                                    actionText = actionText,
                                    actionType = actionType
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            res.aiRationale?.let { rationale ->
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = "Rationale",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "OPTIMIZER RATIONALE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = rationale,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // Error State Card
            if (errorMessage != null) {
                item(key = "error_section") {
                    ErrorStateCard(
                        message = errorMessage!!,
                        onRetry = {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    optimizationResult = repository.getOptimization(universe, if (views.isNotEmpty()) views else null)
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    )
                }
            }

            // 4. Universe Configuration & Investor Views Input
            item(key = "config_section") {
                DoubleBezelCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Investment universe & investor views",
                            supportingText = "Add target assets and specify subjective expected return views"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TerminalTextField(
                                value = tickerInput,
                                onValueChange = { tickerInput = it },
                                label = "Asset Symbol",
                                placeholder = "e.g. NVDA",
                                modifier = Modifier.weight(1.4f)
                            )

                            TerminalTextField(
                                value = viewInput,
                                onValueChange = { viewInput = it },
                                label = "Exp. Return (Decimal)",
                                placeholder = "e.g. 0.16",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SecondaryButton(
                                text = "Add Asset",
                                onClick = {
                                    val ticker = tickerInput.trim().uppercase()
                                    val expectedReturn = viewInput.toDoubleOrNull()
                                    if (ticker.isNotEmpty()) {
                                        if (!universe.contains(ticker)) universe = universe + ticker
                                        if (expectedReturn != null) views = views + (ticker to expectedReturn)
                                        tickerInput = ""
                                        viewInput = ""
                                    }
                                },
                                icon = Icons.Default.Add,
                                modifier = Modifier.weight(1f)
                            )

                            PrimaryButton(
                                text = "Solve Weights",
                                onClick = {
                                    coroutineScope.launch {
                                        if (universe.isEmpty()) {
                                            errorMessage = "Please add assets to your investment universe."
                                            return@launch
                                        }
                                        isLoading = true
                                        errorMessage = null
                                        optimizationResult = null
                                        try {
                                            optimizationResult = repository.getOptimization(universe, if (views.isNotEmpty()) views else null)
                                        } catch (e: Exception) {
                                            errorMessage = "Solver engine unreachable: ${e.message ?: "Verify optimizer service"}"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                isLoading = isLoading,
                                modifier = Modifier.weight(1.4f)
                            )
                        }

                        if (universe.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            MetadataLabel(text = "Universe: ${universe.joinToString(", ")}")
                            if (views.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Investor Views: ${views.map { "${it.key} = ${(it.value * 100).toInt()}%" }.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 5. Model Transparency Disclaimer
            item(key = "disclaimer_section") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Disclaimer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Model Transparency: Black-Litterman and Mean-Variance optimizations depend heavily on covariance matrix estimations and subjective return inputs. A mathematical optimum does not guarantee superior returns or protect against market shifts.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * RebalanceActionCard: Card showing current weight vs target weight and rebalance action badge.
 */
@Composable
fun RebalanceActionCard(
    ticker: String,
    currentWeight: Double,
    targetWeight: Double,
    actionText: String,
    actionType: StatusBadgeType
) {
    val animatedTargetWeight by animateFloatAsState(
        targetValue = targetWeight.toFloat().coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = .82f, stiffness = Spring.StiffnessLow),
        label = "target_weight_$ticker"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = ticker,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Current: ${String.format(Locale.US, "%.1f", currentWeight * 100)}% → Target: ${String.format(Locale.US, "%.1f", targetWeight * 100)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(text = actionText, type = actionType)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedTargetWeight)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
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

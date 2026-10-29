package com.example.prototyx.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.*
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.theme.Mint
import com.example.prototyx.theme.Coral
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch

data class PortfolioHealthReport(
    val score: Int,
    val status: String,
    val statusColor: Color,
    val diagnosis: String
)

@Composable
fun DashboardScreen(
    repository: DataRepository,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    authManager: com.example.prototyx.data.security.AuthManager? = null,
    onVoiceTap: () -> Unit = {},
    onExplore: () -> Unit = {},
    onResearch: (String) -> Unit = {},
    onEarnings: (String) -> Unit = {},
    onRisk: () -> Unit = {},
    onOptimize: () -> Unit = {},
    onEditHolding: (String) -> Unit = {}
) {
    var showDisclaimer by remember { mutableStateOf(authManager?.hasAcceptedDisclaimer() == false) }

    if (showDisclaimer) {
        FinancialDisclaimerDialog(
            onAccept = {
                authManager?.setAcceptedDisclaimer(true)
                showDisclaimer = false
            }
        )
    }
    val holdings by repository.holdings.collectAsState(initial = emptyMap())
    val coroutineScope = rememberCoroutineScope()
    var selectedTicker by remember { mutableStateOf("") }
    var chartPeriod by remember { mutableStateOf("1M") }
    var tickerData by remember { mutableStateOf<MarketIndicatorsResponse?>(null) }
    var chartData by remember { mutableStateOf<StockChartResponse?>(null) }
    var newsFeed by remember { mutableStateOf<List<NewsSentimentItem>>(emptyList()) }
    var finnyReport by remember { mutableStateOf<FinnyStrategyReport?>(null) }

    var userMode by remember { mutableStateOf(UserExperienceMode.PRO) }
    var isLoading by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var syncError by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var searchWeight by remember { mutableStateOf("") }

    val activeTickers = remember(holdings) { holdings.keys.toList() }
    val currentWeights = remember(holdings) { holdings.values.toList() }

    val defaultColors = listOf(
        Color(0xFF111111), Color(0xFF787774), Color(0xFFC0C0C0), Color(0xFFE1F3FE),
        Color(0xFF1F6C9F), Color(0xFF4CAF50), Color(0xFF9C27B0), Color(0xFFFF9800)
    )

    var newsLoading by remember { mutableStateOf(false) }
    var newsError by remember { mutableStateOf<String?>(null) }
    var newsRefresh by remember { mutableIntStateOf(0) }
    var showEditor by remember { mutableStateOf(false) }

    LaunchedEffect(activeTickers, newsRefresh) {
        newsFeed = emptyList()
        newsError = null
        if (activeTickers.isNotEmpty()) {
            newsLoading = true
            try {
                val articles = mutableListOf<NewsSentimentItem>()
                activeTickers.take(4).forEach { ticker ->
                    try { articles.addAll(repository.getNewsSentiment(ticker)) }
                    catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (_: Exception) { newsError = "Some feeds could not be refreshed." }
                }
                newsFeed = articles.groupBy { it.title.lowercase() }.values.map { group ->
                    group.first().copy(tickers = group.flatMap { it.tickers }.distinct())
                }.sortedByDescending { it.timestamp }.take(8)
            } finally { newsLoading = false }
        }
    }

    LaunchedEffect(selectedTicker, chartPeriod) {
        if (selectedTicker.isNotEmpty()) {
            isLoading = true
            chartData = null
            tickerData = null
            searchError = null
            try {
                tickerData = repository.getIndicators(selectedTicker)
                chartData = repository.getStockChart(selectedTicker, chartPeriod)
            } catch (e: Exception) {
                searchError = "Could not load $selectedTicker: ${e.message ?: "Unknown error"}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(holdings) {
        if (selectedTicker !in holdings) {
            selectedTicker = holdings.keys.firstOrNull().orEmpty()
        }
    }

    val palette = listOf(Color(0xFF344D35), Color(0xFF8F9C78), Color(0xFFC0A879), Color(0xFF697F85))
    val invested = holdings.values.sum()
    LazyColumn(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                EditorialHeading("Portfolio", modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    coroutineScope.launch {
                        isSyncing = true
                        try { repository.syncWithCloud(); syncError = null; newsRefresh++ }
                        catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (e: Exception) { syncError = e.message }
                        finally { isSyncing = false }
                    }
                }, enabled = !isSyncing) {
                    if (isSyncing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, contentDescription = "Sync portfolio")
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                MetadataLabel("TRACKED ALLOCATION")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(String.format("%.0f", invested * 100), fontSize = 38.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        letterSpacing = (-1).sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("%", fontSize = 24.sp, modifier = Modifier.padding(bottom = 9.dp))
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 9.dp)) {
                        Text("${holdings.size} holdings", style = MaterialTheme.typography.titleMedium)
                        Text("${String.format("%.0f", (1f-invested).coerceAtLeast(0f)*100)}% unallocated",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.fillMaxWidth().height(7.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    holdings.entries.forEachIndexed { index, entry ->
                        if (entry.value > 0) Box(Modifier.weight(entry.value).fillMaxHeight().background(palette[index % palette.size]))
                    }
                    if (invested < 1f) Box(Modifier.weight((1f-invested).coerceAtLeast(.001f)).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    MetadataLabel("HOLDING", Modifier.weight(1f))
                    MetadataLabel("WEIGHT")
                    TextButton(onClick = { showEditor = !showEditor }) { Text(if (showEditor) "Close" else "Edit") }
                }
                Column {
                holdings.entries.forEachIndexed { index, entry ->
                    Row(Modifier.fillMaxWidth().clickable { selectedTicker = entry.key }
                        .background(if (selectedTicker == entry.key) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .5f) else Color.Transparent)
                        .padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(palette[index % palette.size]))
                        Spacer(Modifier.width(12.dp))
                        Text(entry.key, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(String.format("%.1f%%", entry.value*100), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 15.sp)
                        Text("  →", color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                }
                if (holdings.isEmpty()) {
                    Text("Build your first portfolio", style = MaterialTheme.typography.titleLarge)
                    Text("Find a company, review its business, then choose an allocation. Your portfolio tracks weights; it does not place trades.", style = MaterialTheme.typography.bodyMedium)
                    PrimaryButton("Find a company", onClick = onExplore, modifier = Modifier.fillMaxWidth())
                } else {
                    Row {
                        TextButton(onClick = onExplore) { Text("Add company") }
                        TextButton(onClick = onRisk) { Text("Check risk") }
                        TextButton(onClick = onOptimize) { Text("Rebalance") }
                    }
                }
            }
        }
        if (showEditor) item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Edit allocation", supportingText = "Enter a percentage, e.g. 20 for 20%. Use 0 to remove.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("Ticker") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(searchWeight, { searchWeight = it }, label = { Text("Weight (%)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                PrimaryButton("Save allocation", onClick = {
                    val ticker = searchQuery.trim().uppercase()
                    val weight = searchWeight.toFloatOrNull()
                    if (ticker.isBlank() || weight == null || !weight.isFinite() || weight !in 0f..100f) {
                        searchError = "Enter a ticker and a percentage from 0 to 100."
                    } else coroutineScope.launch {
                        try {
                            if (weight == 0f) repository.removeHolding(ticker) else repository.updateHolding(ticker, weight / 100f)
                            searchQuery = ""; searchWeight = ""; searchError = null
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (e: Exception) { searchError = e.message }
                    }
                }, modifier = Modifier.fillMaxWidth())
            }
        }
        if (syncError != null || searchError != null) item { StatusMessage(syncError ?: searchError.orEmpty(), isError = true) }
        if (selectedTicker.isNotEmpty()) item {
            Text(selectedTicker, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            MetadataLabel("COMPANY TOOLS")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onResearch(selectedTicker) }) { Text("Research") }
                TextButton(onClick = { onEarnings(selectedTicker) }) { Text("Earnings") }
                TextButton(onClick = { onEditHolding(selectedTicker) }) { Text("Allocation") }
            }
            if (isLoading) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("Loading $selectedTicker price history…", style = MaterialTheme.typography.bodySmall)
            }
            chartData?.let { data -> StockChartView(data.ticker, data.pricePoints, selectedPeriod = chartPeriod, onPeriodChanged = { chartPeriod = it }, showTicker = false) }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Holdings in the news", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { newsRefresh++ }, enabled = !newsLoading) { Text("Refresh") }
            }
            Text(if (activeTickers.isEmpty()) "Add holdings to see company headlines." else
                "Dated headlines matched to ${activeTickers.take(4).joinToString(" · ")}. Impact is not assessed.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (newsLoading) { Spacer(Modifier.height(12.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
            newsError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            if (!newsLoading && newsFeed.isEmpty() && activeTickers.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("No matching headlines from the past 7 days.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        items(newsFeed, key = { it.id }) { NewsSentimentCard(it) }
        item { MetadataLabel("PROTOTYX / RESEARCH, NOT EXECUTION"); Spacer(Modifier.height(12.dp)) }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    PrototyxTheme { DashboardScreen(MockDataRepository(), {}) }
}

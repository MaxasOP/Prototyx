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

data class PresetBasket(
    val title: String,
    val icon: String,
    val subtitle: String,
    val holdings: Map<String, Float>
)

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
    onVoiceTap: () -> Unit = {}
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
    var pendingBasket by remember { mutableStateOf<PresetBasket?>(null) }

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

    val demoBaskets = remember {
        listOf(
            PresetBasket("Technology", "", "Growth", mapOf("NVDA" to 0.35f, "AAPL" to 0.35f, "MSFT" to 0.30f)),
            PresetBasket("Core India", "", "Balanced", mapOf("RELIANCE" to 0.40f, "TCS" to 0.30f, "HDFCBANK" to 0.30f)),
            PresetBasket("Defensive", "", "Income", mapOf("ITC" to 0.40f, "HINDUNILVR" to 0.35f, "TCS" to 0.25f))
        )
    }

    val palette = listOf(Color(0xFF344D35), Color(0xFF8F9C78), Color(0xFFC0A879), Color(0xFF697F85))
    val invested = holdings.values.sum()
    if (pendingBasket != null) {
        val basket = pendingBasket!!
        AlertDialog(onDismissRequest = { pendingBasket = null },
            title = { Text("Use ${basket.title}?") },
            text = { Text("This replaces your current allocation with the model weights. It does not place trades.") },
            confirmButton = { TextButton(onClick = {
                pendingBasket = null
                coroutineScope.launch {
                    try { repository.replaceHoldings(basket.holdings); selectedTicker = basket.holdings.keys.first() }
                    catch (e: kotlinx.coroutines.CancellationException) { throw e }
                    catch (e: Exception) { syncError = e.message }
                }
            }) { Text("Replace allocation") } },
            dismissButton = { TextButton(onClick = { pendingBasket = null }) { Text("Keep portfolio") } })
    }
    LazyColumn(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Portfolio", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
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
                IconButton(onClick = { coroutineScope.launch { repository.logout(); onLogout() } }) {
                    Icon(Icons.AutoMirrored.Default.ExitToApp, contentDescription = "Sign out", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                MetadataLabel("ALLOCATION / INVESTED CAPITAL")
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(String.format("%.0f", invested * 100), fontSize = 54.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        letterSpacing = (-3).sp, color = MaterialTheme.colorScheme.onSurface)
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
                if (holdings.isEmpty()) Text("No holdings yet. Add an allocation or choose a model below.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (showEditor || holdings.isEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader("Edit allocation", supportingText = "Decimal weight: 0.2 = 20%. Use 0 to remove.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(searchQuery, { searchQuery = it }, label = { Text("Ticker") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(searchWeight, { searchWeight = it }, label = { Text("Weight") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                PrimaryButton("Save allocation", onClick = {
                    val ticker = searchQuery.trim().uppercase()
                    val weight = searchWeight.toFloatOrNull()
                    if (ticker.isBlank() || weight == null || !weight.isFinite() || weight !in 0f..1f) {
                        searchError = "Enter a ticker and a decimal weight from 0 to 1."
                    } else coroutineScope.launch {
                        try {
                            if (weight == 0f) repository.removeHolding(ticker) else repository.updateHolding(ticker, weight)
                            searchQuery = ""; searchWeight = ""; searchError = null
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                        catch (e: Exception) { searchError = e.message }
                    }
                }, modifier = Modifier.fillMaxWidth())
            }
        }
        if (syncError != null || searchError != null) item { StatusMessage(syncError ?: searchError.orEmpty(), isError = true) }
        if (selectedTicker.isNotEmpty()) item {
            if (isLoading) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("Loading $selectedTicker price history…", style = MaterialTheme.typography.bodySmall)
            }
            chartData?.let { data -> StockChartView(data.ticker, data.pricePoints, selectedPeriod = chartPeriod, onPeriodChanged = { chartPeriod = it }) }
        }
        item {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(18.dp))
            SectionHeader("Model allocations", supportingText = "Reference weights. Selecting a model replaces your holdings.")
            demoBaskets.forEach { basket ->
                Row(Modifier.fillMaxWidth().clickable { pendingBasket = basket }.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(basket.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(basket.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("   →", color = MaterialTheme.colorScheme.primary)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
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

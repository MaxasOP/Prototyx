package com.example.prototyx.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.model.TickerSearchResponse
import com.example.prototyx.data.model.MarketIndicatorsResponse
import com.example.prototyx.ui.components.EditorialHeading
import com.example.prototyx.ui.components.MetadataLabel
import kotlinx.coroutines.launch

@Composable
fun ScreenerScreen(repository: DataRepository, modifier: Modifier = Modifier,
    onResearch: (String) -> Unit = {}, onEarnings: (String) -> Unit = {}, onAddHolding: (String) -> Unit = {}) {
    var query by rememberSaveable { mutableStateOf("") }
    var known by remember { mutableStateOf<List<TickerSearchResponse>>(emptyList()) }
    var remote by remember { mutableStateOf<List<TickerSearchResponse>>(emptyList()) }
    var remoteQuery by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val normalized = query.trim()
    val local = known.filter { normalized.isBlank() || it.ticker.contains(normalized, true) || it.name.contains(normalized, true) }
    val visible = (local + if (remoteQuery == normalized) remote else emptyList()).distinctBy { it.ticker }
    LaunchedEffect(normalized, retry) {
        loading = true; error = null
        try {
            if (normalized.isNotBlank()) kotlinx.coroutines.delay(300)
            val found = repository.searchTickers(normalized)
            remote = found; remoteQuery = normalized
            known = (known + found).distinctBy { it.ticker }.takeLast(200)
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { error = "Could not search more companies. Check your connection and retry." }
        finally { loading = false }
    }
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditorialHeading("Explore")
        Text("Search Indian and international companies by name or ticker.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(query, { query = it }, label = { Text("Company or ticker") },
            placeholder = { Text("e.g. HDFC Bank, Tesla, SBIN.NS") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { retry++ }) { Text("Retry search") } }
        MetadataLabel(if (normalized.isBlank()) "POPULAR COMPANIES · SEARCH FOR MORE" else "${visible.size} MATCHES" + if (loading) " · SEARCHING MORE" else "")
        if (!loading && error == null && visible.isEmpty()) Text("No companies found. Try the company name or exchange ticker.")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(visible, key = { it.ticker }) { company ->
                ExploreCompany(company, repository, { onResearch(company.ticker) }, { onEarnings(company.ticker) }, { onAddHolding(company.ticker) })
            }
        }
    }
}

@Composable
private fun ExploreCompany(company: TickerSearchResponse, repository: DataRepository, onResearch: () -> Unit, onEarnings: () -> Unit, onAllocate: () -> Unit) {
    val scope = rememberCoroutineScope()
    val detailed = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.example.prototyx.PrototyxApp).preferences.detailed
    var snapshot by remember(company.ticker) { mutableStateOf<MarketIndicatorsResponse?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(company.name, style = MaterialTheme.typography.titleMedium)
        Text(listOf(company.ticker, company.exchange).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        snapshot?.let {
            Text("%.2f".format(it.close) + " " + (it.currency ?: ""), style = MaterialTheme.typography.titleLarge)
            if (detailed) Text("P/E: ${it.peRatio?.let { value -> "%.1f".format(value) } ?: "—"}   RSI: ${it.rsi?.let { value -> "%.0f".format(value) } ?: "—"}", style = MaterialTheme.typography.bodySmall)
            Text(it.asOf?.let { date -> "Market snapshot · ${date.take(10)}" } ?: "Latest available market snapshot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = onResearch, modifier = Modifier.weight(1f)) { Text("Research") }
            TextButton(onClick = onEarnings, modifier = Modifier.weight(1f)) { Text("Earnings") }
            TextButton(onClick = onAllocate, modifier = Modifier.weight(1f)) { Text("Allocate") }
        }
        if (snapshot == null) TextButton(enabled = !loading, onClick = { scope.launch {
            loading = true; error = null
            try { snapshot = repository.getIndicators(company.ticker) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { error = "Market snapshot unavailable. You can retry." }
            finally { loading = false }
        } }) { Text(if (loading) "Loading snapshot…" else "View market snapshot") }
    }
}

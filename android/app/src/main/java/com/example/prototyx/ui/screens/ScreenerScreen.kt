package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.model.ScreenerResultItem
import com.example.prototyx.data.model.UserExperienceMode
import com.example.prototyx.ui.components.DoubleBezelCard
import com.example.prototyx.ui.components.EditorialHeading
import com.example.prototyx.ui.components.MetadataLabel
import com.example.prototyx.ui.components.ModeToggleBar

@Composable
fun ScreenerScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier,
    onResearch: (String) -> Unit = {},
    onEarnings: (String) -> Unit = {},
    onAddHolding: (String) -> Unit = {}
) {
    val appPreferences = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.example.prototyx.PrototyxApp).preferences
    var queryText by rememberSaveable { mutableStateOf("") }
    var userMode by remember(appPreferences.detailed) { mutableStateOf(if(appPreferences.detailed) UserExperienceMode.PRO else UserExperienceMode.SIMPLE) }
    var results by remember { mutableStateOf<List<ScreenerResultItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(queryText) {
        isLoading = true
        errorMessage = null
        try {
            kotlinx.coroutines.delay(350)
            results = repository.searchScreener(queryText)
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) {
            results = emptyList()
            errorMessage = "Search unavailable. Check your connection and try again."
        } finally { isLoading = false }
    }

    val presetQueries = listOf(
        "TCS",
        "AAPL"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        EditorialHeading(text = "Explore")
        Text("Find a company. Review the evidence. Decide what belongs in your portfolio.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(12.dp))

        ModeToggleBar(
            currentMode = userMode,
            onModeChanged = { userMode = it; appPreferences.updateDetailed(it == UserExperienceMode.PRO) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Input
        OutlinedTextField(
            value = queryText,
            onValueChange = { queryText = it },
            placeholder = { Text("Company or ticker, e.g. AAPL or TCS", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presetQueries.take(2).forEach { preset ->
                AssistChip(
                    onClick = { queryText = preset.replace(Regex("[^a-zA-Z ]"), "").trim() },
                    label = { Text(preset, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!isLoading && results.isEmpty() && errorMessage == null) {
            Text("No supported companies match this search.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results) { item ->
                    ScreenerResultCard(item = item, userMode = userMode, onResearch = { onResearch(item.ticker) }, onEarnings = { onEarnings(item.ticker) }, onAddHolding = { onAddHolding(item.ticker) })
                }
            }
        }
    }
}

@Composable
fun ScreenerResultCard(item: ScreenerResultItem, userMode: UserExperienceMode, onResearch: () -> Unit = {}, onEarnings: () -> Unit = {}, onAddHolding: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(item.ticker, style = MaterialTheme.typography.titleLarge)
                Text(item.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(String.format("%.2f", item.price), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 20.sp)
                Text(item.currency ?: "Currency unavailable", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (userMode == UserExperienceMode.PRO) {
            Row(Modifier.fillMaxWidth()) {
                listOf("P/E" to (item.peRatio?.let { String.format("%.1f", it) } ?: "—"),
                    "YIELD" to (item.dividendYield?.let { "$it%" } ?: "—"),
                    "RSI" to (item.rsi?.let { String.format("%.0f", it) } ?: "—")).forEach { (label, value) ->
                    Column(Modifier.weight(1f)) {
                        MetadataLabel(label)
                        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }
        Text(if (userMode == UserExperienceMode.SIMPLE) item.plainEnglishSummary else item.aiAnalysis,
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onResearch, modifier = Modifier.weight(1f)) { Text("Research") }
            TextButton(onClick = onEarnings, modifier = Modifier.weight(1f)) { Text("Earnings") }
            TextButton(onClick = onAddHolding, modifier = Modifier.weight(1f)) { Text("Allocate") }
        }
    }
}

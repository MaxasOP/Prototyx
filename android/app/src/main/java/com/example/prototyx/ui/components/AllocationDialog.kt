package com.example.prototyx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.prototyx.data.DataRepository
import kotlinx.coroutines.launch

@Composable
fun AllocationDialog(ticker: String, repository: DataRepository, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val holdings by repository.holdings.collectAsState(initial = emptyMap())
    var percent by remember(ticker) { mutableStateOf("") }
    var initialized by remember(ticker) { mutableStateOf(false) }
    LaunchedEffect(holdings) {
        if (!initialized && holdings.containsKey(ticker)) {
            percent = "%g".format(java.util.Locale.US, holdings.getValue(ticker) * 100)
            initialized = true
        }
    }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val available = (100f - holdings.filterKeys { it != ticker }.values.sum() * 100).coerceAtLeast(0f)
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("$ticker allocation") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Set the share of your tracked portfolio. This does not place a trade.")
            Text("Up to %.1f%% available. Enter 0 to remove this holding.".format(available), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(percent, { percent = it; error = null }, label = { Text("Portfolio weight (%)") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = !busy, onClick = {
            val value = percent.toFloatOrNull()
            if (value == null || !value.isFinite() || value !in 0f..100f) error = "Enter a percentage from 0 to 100."
            else if (value > available + .001f) error = "Reduce another holding first. Only %.1f%% is available.".format(available)
            else scope.launch {
                busy = true
                try {
                    if (value == 0f) repository.removeHolding(ticker) else repository.updateHolding(ticker, value / 100f)
                    onSaved()
                } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                catch (e: Exception) { error = e.message ?: "Could not save allocation. Try again." }
                finally { busy = false }
            }
        }) { Text(if (busy) "Saving…" else "Save allocation") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel") } })
}

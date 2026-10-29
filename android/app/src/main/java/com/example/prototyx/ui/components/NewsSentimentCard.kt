package com.example.prototyx.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.prototyx.data.model.NewsSentimentItem
import com.example.prototyx.data.model.UserExperienceMode
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun NewsSentimentCard(item: NewsSentimentItem, modifier: Modifier = Modifier, userMode: UserExperienceMode = UserExperienceMode.PRO) {
    val handler = LocalUriHandler.current
    val clickable = item.url?.startsWith("https://") == true
    val date = runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(item.timestamp)
        SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(parsed!!)
    }.getOrDefault(item.timestamp)
    Column(modifier.fillMaxWidth().clickable(enabled = clickable) { item.url?.let { handler.openUri(it) } }
        .padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            MetadataLabel(item.tickers.joinToString(" / ").ifBlank { "MARKET" }, Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(item.source, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            if (clickable) Text("Read source →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

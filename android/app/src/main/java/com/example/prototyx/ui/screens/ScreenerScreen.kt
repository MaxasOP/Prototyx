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
    modifier: Modifier = Modifier
) {
    var queryText by remember { mutableStateOf("") }
    var userMode by remember { mutableStateOf(UserExperienceMode.PRO) }
    var results by remember { mutableStateOf<List<ScreenerResultItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(queryText) {
        isLoading = true
        results = repository.searchScreener(queryText)
        isLoading = false
    }

    val presetQueries = listOf(
        "Dividend quality",
        "High growth technology",
        "Undervalued large caps",
        "Low RSI momentum"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        EditorialHeading(text = "Equity screen")
        MetadataLabel(text = "SEARCH BY COMPANY, METRIC OR INVESTMENT STYLE")

        Spacer(modifier = Modifier.height(12.dp))

        ModeToggleBar(
            currentMode = userMode,
            onModeChanged = { userMode = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search Input
        OutlinedTextField(
            value = queryText,
            onValueChange = { queryText = it },
            placeholder = { Text("Search stocks, e.g. 'Safe tech stocks with P/E < 30'...", fontSize = 12.sp) },
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
                    ScreenerResultCard(item = item, userMode = userMode)
                }
            }
        }
    }
}

@Composable
fun ScreenerResultCard(
    item: ScreenerResultItem,
    userMode: UserExperienceMode
) {
    val (riskColor, riskText) = when (item.riskLevel.uppercase()) {
        "SAFE" -> MaterialTheme.colorScheme.tertiary to "LOW RISK"
        "BALANCED" -> com.example.prototyx.theme.Amber to "BALANCED"
        else -> MaterialTheme.colorScheme.error to "HIGH RISK"
    }

    DoubleBezelCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = item.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "${item.ticker} • ${item.sector}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Box(
                    modifier = Modifier
                        .background(riskColor.copy(alpha = .12f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(text = riskText, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = riskColor)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (userMode == UserExperienceMode.PRO) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Price: ₹${item.price}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "P/E: ${item.peRatio}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Yield: ${item.dividendYield}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "RSI: ${item.rsi}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = if (userMode == UserExperienceMode.SIMPLE) item.plainEnglishSummary else item.aiAnalysis,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

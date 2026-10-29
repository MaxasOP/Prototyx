package com.example.prototyx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.model.UserExperienceMode

@Composable
fun ModeToggleBar(
    currentMode: UserExperienceMode,
    onModeChanged: (UserExperienceMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(UserExperienceMode.SIMPLE to "Overview", UserExperienceMode.PRO to "Detailed").forEach { (mode, title) ->
            Column(Modifier.weight(1f)) {
                TextButton(onClick = { onModeChanged(mode) }, modifier = Modifier.fillMaxWidth()) {
                    Text(title, color = if (currentMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(thickness = if (currentMode == mode) 2.dp else 1.dp,
                    color = if (currentMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
fun SimpleRiskGauge(
    status: String, // "SAFE", "BALANCED", "GROWTH"
    modifier: Modifier = Modifier
) {
    val (color, label, description) = when (status.uppercase()) {
        "SAFE" -> Triple(MaterialTheme.colorScheme.tertiary, "Low risk · Defensive", "Stable investments focused on capital preservation and steady returns.")
        "BALANCED" -> Triple(com.example.prototyx.theme.Amber, "Balanced growth", "Moderate risk spread across growing tech and dividend leaders.")
        else -> Triple(MaterialTheme.colorScheme.error, "High growth · Volatile", "High return potential with higher daily price movement.")
    }

    DoubleBezelCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun TapToExplainTooltip(
    termTitle: String,
    simpleExplanation: String,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .clickable { showDialog = true }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = termTitle,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "Explain",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp)
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(text = "About $termTitle", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(text = simpleExplanation, fontSize = 13.sp, lineHeight = 18.sp)
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

package com.example.prototyx.ui.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.theme.Mint
import com.example.prototyx.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCoPilotSheet(
    voiceManager: VoiceAssistantManager,
    repository: DataRepository,
    onDismiss: () -> Unit
) {
    var manualInput by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val quickQuestions = listOf(
        "Accumulate Reliance or TCS?",
        "Assess IT sector risk concentration",
        "Optimal Sharpe rebalance strategy",
        "Nvidia enterprise AI outlook"
    )

    // Pulse animation when listening
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (voiceManager.isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    ModalBottomSheet(
        onDismissRequest = {
            voiceManager.stopSpeaking()
            voiceManager.stopListening()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FINNY VOICE CO-PILOT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ask about portfolio risk or asset thesis",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TerminalIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close",
                    onClick = {
                        voiceManager.stopSpeaking()
                        voiceManager.stopListening()
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Microphone Orb
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulseScale)
                    .background(
                        color = when {
                            voiceManager.isListening -> MaterialTheme.colorScheme.error
                            voiceManager.isSpeaking -> Mint
                            voiceManager.isThinking -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        shape = CircleShape
                    )
                    .clickable {
                        if (voiceManager.isListening) {
                            voiceManager.stopListening()
                            if (voiceManager.recognizedText.isNotBlank()) {
                                voiceManager.queryAssistant(voiceManager.recognizedText, repository)
                            }
                        } else {
                            voiceManager.startListening()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Voice Search",
                        tint = when {
                            voiceManager.isListening || voiceManager.isSpeaking || voiceManager.isThinking -> Color.White
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            voiceManager.isListening -> "MIC ON"
                            voiceManager.isSpeaking -> "TALKING"
                            voiceManager.isThinking -> "THINKING"
                            else -> "VOICE"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            voiceManager.isListening || voiceManager.isSpeaking || voiceManager.isThinking -> Color.White
                            else -> MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Message
            Text(
                text = voiceManager.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Captured Speech Output Box
            if (voiceManager.recognizedText.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                DoubleBezelCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MetadataLabel(text = "CAPTURED VOICE QUERY", color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${voiceManager.recognizedText}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // AI Orchestrator Response Box
            voiceManager.responseText?.let { response ->
                Spacer(modifier = Modifier.height(16.dp))
                DoubleBezelCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(
                                text = "RESEARCH VIEW · ${voiceManager.targetAsset ?: "PORTFOLIO"}",
                                type = StatusBadgeType.POSITIVE
                            )

                            if (voiceManager.isSpeaking) {
                                TerminalTextButton(
                                    text = "Stop Voice",
                                    onClick = { voiceManager.stopSpeaking() },
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                SecondaryButton(
                                    text = "Replay",
                                    onClick = { voiceManager.speak(response) },
                                    icon = Icons.Default.PlayArrow
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = response,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Verified Source",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Source: Finny Multi-Agent Orchestrator (Grok LPUs)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Quick Question Chips Carousel
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                MetadataLabel(text = "SUGGESTED RESEARCH PROMPTS")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickQuestions.forEach { question ->
                        TerminalFilterChip(
                            selected = false,
                            onClick = {
                                voiceManager.queryAssistant(question, repository)
                            },
                            label = question
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Text Input Fallback
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TerminalTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    placeholder = "Type research question for committee...",
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (manualInput.isNotBlank()) {
                            val q = manualInput
                            manualInput = ""
                            voiceManager.queryAssistant(q, repository)
                        }
                    },
                    enabled = manualInput.isNotBlank() && !voiceManager.isThinking,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (manualInput.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}

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
import com.example.prototyx.ui.components.MetadataLabel

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
        "Should I accumulate Reliance or TCS?",
        "Assess risk on Indian IT sector concentration",
        "What is the optimal Sharpe rebalance strategy?",
        "Give me an executive outlook on Nvidia"
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
                                .background(Color(0xFF1F6C9F), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FINNY VOICE CO-PILOT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color(0xFF1F6C9F)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ask about your portfolio",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = {
                    voiceManager.stopSpeaking()
                    voiceManager.stopListening()
                    onDismiss()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Microphone Orb
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulseScale)
                    .background(
                        color = when {
                            voiceManager.isListening -> Color(0xFFE53935)
                            voiceManager.isSpeaking -> Color(0xFF2E7D32)
                            voiceManager.isThinking -> Color(0xFF1F6C9F)
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
                Text(
                    text = when {
                        voiceManager.isListening -> "MIC ON"
                        voiceManager.isSpeaking -> "TALKING"
                        voiceManager.isThinking -> "..."
                        else -> "VOICE"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        voiceManager.isListening || voiceManager.isSpeaking || voiceManager.isThinking -> Color.White
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Badge
            Text(
                text = voiceManager.statusMessage,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            // Live Speech / Text Output Box
            if (voiceManager.recognizedText.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        MetadataLabel(text = "Captured Query", color = Color(0xFF1F6C9F))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "\"${voiceManager.recognizedText}\"",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // AI Orchestrator Response Box
            voiceManager.responseText?.let { response ->
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetadataLabel(
                            text = "Research view · ${voiceManager.targetAsset ?: "PORTFOLIO"}",
                            color = Color(0xFF2E7D32)
                        )

                        Row {
                            if (voiceManager.isSpeaking) {
                                TextButton(onClick = { voiceManager.stopSpeaking() }) {
                                    Text("Stop Voice", fontSize = 11.sp, color = Color.Red)
                                }
                            } else {
                                TextButton(onClick = { voiceManager.speak(response) }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Replay", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Replay", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = response,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Quick Questions Horizontal Carousel
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickQuestions.forEach { question ->
                    AssistChip(
                        onClick = {
                            voiceManager.queryAssistant(question, repository)
                        },
                        label = { Text(question, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Text Input Fallback
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    placeholder = { Text("Or type question for committee...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
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
                    enabled = manualInput.isNotBlank() && !voiceManager.isThinking
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

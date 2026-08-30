package com.example.prototyx.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.ConsultResponse
import com.example.prototyx.data.model.DebateResponse
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.theme.SurfaceWhite
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch
import java.util.Locale

enum class CommitteeView {
    Selection, Consultation, Debate
}

@Composable
fun CommitteeScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var currentView by remember { mutableStateOf(CommitteeView.Selection) }
    
    var inputTickers by remember { mutableStateOf("") }
    var userQuery by remember { mutableStateOf("") }
    
    var debateResult by remember { mutableStateOf<DebateResponse?>(null) }
    var consultResult by remember { mutableStateOf<ConsultResponse?>(null) }

    var isLoadingDebate by remember { mutableStateOf(false) }
    var isLoadingConsult by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val viewsList = remember(debateResult) { debateResult?.impliedViews?.toList() ?: emptyList() }

    BackHandler(enabled = currentView != CommitteeView.Selection) {
        currentView = CommitteeView.Selection
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item(key = "heading") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentView != CommitteeView.Selection) {
                        IconButton(
                            onClick = { currentView = CommitteeView.Selection },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    EditorialHeading(
                        text = when (currentView) {
                            CommitteeView.Selection -> "Investment Committee"
                            CommitteeView.Consultation -> "Manager Consultation"
                            CommitteeView.Debate -> "Governance Debate"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                MetadataLabel(text = "Professional Consensus & AI Analysis")
            }
        }

        if (currentView == CommitteeView.Selection) {
            item(key = "tile_consult") {
                SelectionTile(
                    title = "Manager Consultation",
                    description = "Connect directly with our AI Orchestrator to get personalized investment advice. Ask complex natural language questions and receive a synthesized verdict from multiple specialized worker agents.",
                    onClick = { currentView = CommitteeView.Consultation }
                )
            }

            item(key = "tile_debate") {
                SelectionTile(
                    title = "Governance Debate Pool",
                    description = "Initiate a structured deep-dive consensus meeting for specific assets. Watch Fundamental, Technical, and Compliance agents debate pros/cons to reach an implied forward view for your portfolio.",
                    onClick = { currentView = CommitteeView.Debate }
                )
            }
        }

        // --- PART 1: Direct Multi-Agent Consultation (Orchestrator) ---
        if (currentView == CommitteeView.Consultation) {
            item(key = "consult_input") {
                DoubleBezelCard {
                    MetadataLabel(text = "Consultation Parameters", color = Color(0xFF1F6C9F))
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = userQuery,
                        onValueChange = { userQuery = it },
                        label = { Text("Your Investment Thesis or Question (e.g., Outlook for Tech in Q4?)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.1f)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    PrimaryButton(
                        text = "Launch Manager Orchestration",
                        onClick = {
                            coroutineScope.launch {
                                isLoadingConsult = true
                                errorMessage = null
                                consultResult = null
                                debateResult = null // Clear other results for focus
                                try {
                                    consultResult = repository.runConsult(userQuery)
                                } catch (e: Exception) {
                                    errorMessage = "Orchestration failed: ${e.message ?: "Check backend/Groq status"}"
                                } finally {
                                    isLoadingConsult = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = isLoadingConsult
                    )
                }
            }

            errorMessage?.let { msg ->
                item(key = "error_message_consult") {
                    Text(msg, color = Color.Red, fontSize = 14.sp)
                }
            }

            // Display Consultation Result
            consultResult?.let { res ->
                item(key = "consult_verdict") {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MetadataLabel(text = "Manager Verdict: ${res.asset ?: "N/A"}", color = Color(0xFF1F6C9F))
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), RoundedCornerShape(3.dp)))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(20.dp)
                        ) {
                            Text(
                                text = res.recommendation ?: "No recommendation provided.",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp,
                                color = Color.Black.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // --- PART 2: Governance Debate (Tickers) ---
        if (currentView == CommitteeView.Debate) {
            item(key = "debate_input") {
                DoubleBezelCard {
                    MetadataLabel(text = "Debate Pool Parameters")
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = inputTickers,
                        onValueChange = { inputTickers = it },
                        label = { Text("Tickers to Analyze (e.g., RELIANCE, TCS)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.Black.copy(alpha = 0.1f)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    PrimaryButton(
                        text = "Run Committee Debate",
                        onClick = {
                            coroutineScope.launch {
                                isLoadingDebate = true
                                errorMessage = null
                                debateResult = null
                                consultResult = null
                                try {
                                    val tickers = inputTickers.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                                    debateResult = repository.runDebate(tickers)
                                } catch (e: Exception) {
                                    errorMessage = "Orchestration failed: ${e.message ?: "Verify agent server status"}"
                                } finally {
                                    isLoadingDebate = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        isLoading = isLoadingDebate
                    )
                }
            }

            errorMessage?.let { msg ->
                item(key = "error_message_debate") {
                    Text(msg, color = Color.Red, fontSize = 14.sp)
                }
            }

            // Display Debate Result
            debateResult?.let { res ->
                item(key = "debate_logs_header") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MetadataLabel(text = "Committee Debate Logs", color = Color(0xFF1F6C9F))
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), RoundedCornerShape(3.dp)))
                    }
                }
                
                itemsIndexed(res.debateLogs ?: emptyList(), key = { index, _ -> "debate_log_$index" }) { _, log ->
                    AgentDialogueCard(log.agent, log.message)
                }

                item(key = "debate_views_header") {
                    MetadataLabel(text = "Projected Annual Returns")
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(viewsList, key = { it.first }) { (ticker, view) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceWhite, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ticker, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val formattedView = remember(view) { String.format(Locale.US, "%.1f%%", view * 100) }
                        Text(
                            text = formattedView,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionTile(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        DoubleBezelCard {
            MetadataLabel(text = title, color = Color(0xFF1F6C9F))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color.Black.copy(alpha = 0.6f),
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "ACCESS MODULE →",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F6C9F),
                letterSpacing = 1.sp
            )
        }
    }
}


@Composable
fun AgentDialogueCard(agent: String?, message: String?) {
    val agentName = agent ?: "Unknown Agent"
    val agentColor = remember(agentName) { getAgentColor(agentName) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceWhite, RoundedCornerShape(12.dp))
            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(agentColor, RoundedCornerShape(3.dp)))
            Spacer(modifier = Modifier.width(8.dp))
            MetadataLabel(text = agentName, color = agentColor)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message ?: "No dialogue content available.",
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 18.sp,
            color = Color.Black.copy(alpha = 0.8f)
        )
    }
}

private fun getAgentColor(agent: String): Color {
    return when {
        agent.contains("Fundamental") -> Color(0xFF346538)
        agent.contains("Technical") -> Color(0xFF956400)
        agent.contains("Compliance") -> Color(0xFF9C27B0)
        else -> Color(0xFF1F6C9F)
    }
}

@Preview(showBackground = true)
@Composable
fun CommitteeScreenPreview() {
    PrototyxTheme {
        CommitteeScreen(repository = MockDataRepository())
    }
}

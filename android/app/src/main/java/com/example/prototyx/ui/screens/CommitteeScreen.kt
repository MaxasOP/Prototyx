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
    modifier: Modifier = Modifier,
    initialTicker: String = "",
    onEarnings: (String) -> Unit = {},
    onAddHolding: (String) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var currentView by remember(initialTicker) { mutableStateOf(if (initialTicker.isBlank()) CommitteeView.Selection else CommitteeView.Consultation) }
    
    var inputTickers by remember(initialTicker) { mutableStateOf(initialTicker) }
    var userQuery by remember(initialTicker) { mutableStateOf(if (initialTicker.isBlank()) "" else "What are the investment case, valuation risks and key uncertainties for $initialTicker?") }
    
    var debateResult by remember { mutableStateOf<DebateResponse?>(null) }
    var consultResult by remember { mutableStateOf<ConsultResponse?>(null) }

    var isLoadingDebate by remember { mutableStateOf(false) }
    var isLoadingConsult by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val viewsList = remember(debateResult) { debateResult?.impliedViews?.toList() ?: emptyList() }

    BackHandler(enabled = initialTicker.isBlank() && currentView != CommitteeView.Selection) {
        currentView = CommitteeView.Selection
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item(key = "heading") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (initialTicker.isBlank() && currentView != CommitteeView.Selection) {
                        IconButton(
                            onClick = { currentView = CommitteeView.Selection },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                    EditorialHeading(
                        text = when (currentView) {
                            CommitteeView.Selection -> "Research desk"
                            CommitteeView.Consultation -> "Ask research"
                            CommitteeView.Debate -> "Compare views"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Questions, evidence and competing perspectives.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (initialTicker.isNotBlank()) item(key = "company_actions") {
            Column {
                MetadataLabel(initialTicker)
                Row {
                    TextButton(onClick = { onEarnings(initialTicker) }) { Text("Read earnings") }
                    TextButton(onClick = { onAddHolding(initialTicker) }) { Text("Set allocation") }
                }
            }
        }
        if (currentView == CommitteeView.Selection) {
            item(key = "earnings_entry") {
                SelectionTile(title = "Read company earnings", description = "Open the original earnings-call transcript for a company and fiscal period.", onClick = { onEarnings("") })
            }
            item(key = "tile_consult") {
                SelectionTile(
                    title = "Research a thesis",
                    description = "Test an investment question against fundamental, technical and portfolio-risk perspectives.",
                    onClick = { currentView = CommitteeView.Consultation }
                )
            }

            item(key = "tile_debate") {
                SelectionTile(
                    title = "Compare securities",
                    description = "Read technical, fundamental and risk perspectives on the same observed market data.",
                    onClick = { currentView = CommitteeView.Debate }
                )
            }
        }

        // --- PART 1: Direct Multi-Agent Consultation (Orchestrator) ---
        if (currentView == CommitteeView.Consultation) {
            item(key = "consult_input") {
                DoubleBezelCard {
                    MetadataLabel(text = "RESEARCH QUESTION")
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = userQuery,
                        onValueChange = { userQuery = it },
                        label = { Text("Your Investment Thesis or Question (e.g., Outlook for Tech in Q4?)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = "Review thesis",
                        onClick = {
                            coroutineScope.launch {
                                isLoadingConsult = true
                                errorMessage = null
                                consultResult = null
                                debateResult = null // Clear other results for focus
                                try {
                                    consultResult = repository.runConsult(userQuery)
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Research service unavailable"
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
                    StatusMessage(msg, isError = true)
                }
            }

            // Display Consultation Result
            consultResult?.let { res ->
                item(key = "consult_verdict") {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MetadataLabel(text = "VIEW · ${res.asset ?: "N/A"}")
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF4CAF50), RoundedCornerShape(3.dp)))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(20.dp)
                        ) {
                            Text(
                                text = res.recommendation ?: "No recommendation provided.",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
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
                    MetadataLabel(text = "SECURITIES")
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = inputTickers,
                        onValueChange = { inputTickers = it },
                        label = { Text("Tickers to Analyze (e.g., RELIANCE, TCS)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryButton(
                        text = "Compare evidence",
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
                                    errorMessage = e.message ?: "Research service unavailable"
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
                    StatusMessage(msg, isError = true)
                }
            }

            // Display Debate Result
            debateResult?.let { res ->
                item(key = "debate_consensus_badge") {
                    SectionHeader(title = "Research perspectives", supportingText = "${res.mode.orEmpty()}. No calibrated confidence score or trading instruction is implied.")
                }

                item(key = "debate_logs_header") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MetadataLabel(text = "RESEARCH NOTES")
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
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ticker, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        val formattedView = remember(view) { String.format(Locale.US, "%.1f%%", view * 100) }
                        Text(
                            text = formattedView,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Open research →",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
        }
    }
}


@Composable
fun AgentDialogueCard(agent: String?, message: String?) {
    val agentName = agent ?: "Unknown Agent"
    val agentColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp)
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
            fontFamily = FontFamily.SansSerif,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ExecutiveConsensusCard(
    actionVerdict: String,
    badgeColor: Color,
    confidence: Int,
    takeaway: String
) {
    DoubleBezelCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MetadataLabel(text = "HOUSE VIEW")
            Surface(
                color = badgeColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = actionVerdict,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = badgeColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$confidence% agreement",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                MetadataLabel(text = "CROSS-DISCIPLINE CONFIDENCE")
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(badgeColor, RoundedCornerShape(5.dp))
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = takeaway,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 19.sp
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

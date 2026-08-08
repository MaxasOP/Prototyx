package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.prototyx.data.model.DebateResponse
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun CommitteeScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var inputTickers by remember { mutableStateOf("TCS, RELIANCE") }
    
    var debateResult by remember { mutableStateOf<DebateResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (debateResult == null) {
            try {
                debateResult = repository.runDebate(listOf("TCS", "RELIANCE"))
            } catch (e: Exception) {}
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            Column {
                EditorialHeading(text = "Committee Logic & Governance")
                Spacer(modifier = Modifier.height(8.dp))
                MetadataLabel(text = "Autonomous Multi-Agent Consensus Desk")
            }
        }

        item {
            DoubleBezelCard {
                MetadataLabel(text = "Committee Mandate")
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = inputTickers,
                    onValueChange = { inputTickers = it },
                    label = { Text("Asset Debate Pool", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Initiate Governance Debate",
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            debateResult = null
                            try {
                                val tickers = inputTickers.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                                debateResult = repository.runDebate(tickers)
                            } catch (e: Exception) {
                                errorMessage = "Orchestration failed. Verify agent server status."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isLoading = isLoading
                )
            }
        }

        debateResult?.let { res ->
            item {
                Column {
                    MetadataLabel(text = "Consensus Solver: ${res.mode}")
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    res.debateLogs.forEach { log ->
                        AgentDialogueCard(log.agent, log.message)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            item {
                DoubleBezelCard {
                    MetadataLabel(text = "Implied Forward Estimates")
                    Spacer(modifier = Modifier.height(24.dp))
                    res.impliedViews.forEach { (ticker, view) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(ticker, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${String.format("%.1f", view * 100)}%", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                        }
                        HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}

@Composable
fun AgentDialogueCard(agent: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(getAgentColor(agent), RoundedCornerShape(3.dp)))
            Spacer(modifier = Modifier.width(8.dp))
            MetadataLabel(text = agent, color = getAgentColor(agent))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
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

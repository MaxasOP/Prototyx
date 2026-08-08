package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Explainer
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "AI Investment Committee Desk",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Launches 4 autonomous agents (Macro, Fundamental, Technical, Compliance) in CrewAI to debate asset weights.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Action Trigger Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputTickers,
                        onValueChange = { inputTickers = it },
                        label = { Text("Debate Asset Pool (comma separated)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                debateResult = null
                                try {
                                    val tickers = inputTickers.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
                                    debateResult = repository.runDebate(tickers)
                                } catch (e: Exception) {
                                    errorMessage = "Orchestration failed. Ensure FastAPI local server is running."
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("Kickoff CrewAI Investment Debate")
                        }
                    }
                }
            }
        }

        errorMessage?.let { error ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Text(
                        text = error,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        debateResult?.let { res ->
            item {
                Text(
                    text = "Consensus Solver Mode: ${res.mode}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Chat transcripts loop
            item {
                Text(text = "Committee Dialogue Logs", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            items(res.debateLogs) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(getAgentBadgeColor(log.agent), shape = RoundedCornerShape(4.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = log.agent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = getAgentBadgeColor(log.agent)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = log.message,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Summary return forecasts
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Implied Return Forecasts (Active Views)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        res.impliedViews.forEach { (ticker, view) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(ticker, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("${String.format("%.1f", view * 100)}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getAgentBadgeColor(agentName: String): Color {
    return when {
        agentName.contains("Macro") -> Color(0xFF1E88E5) // Blue
        agentName.contains("Fundamental") -> Color(0xFF4CAF50) // Green
        agentName.contains("Technical") -> Color(0xFFFF9800) // Orange
        else -> Color(0xFF9C27B0) // Purple (Compliance)
    }
}

@Preview(showBackground = true)
@Composable
fun CommitteeScreenPreview() {
    PrototyxTheme {
        CommitteeScreen(repository = MockDataRepository())
    }
}

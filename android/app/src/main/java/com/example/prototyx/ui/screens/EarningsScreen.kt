package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.EarningsTranscriptResponse
import com.example.prototyx.theme.Mint
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun EarningsScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var tickerInput by remember { mutableStateOf("TCS") }
    
    var transcriptResult by remember { mutableStateOf<EarningsTranscriptResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Auto-load initial ticker
    LaunchedEffect(Unit) {
        if (transcriptResult == null && tickerInput.isNotBlank()) {
            try {
                transcriptResult = repository.getTranscript(tickerInput)
            } catch (e: Exception) {
                // Silently fallback to user search
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item(key = "header") {
                ScreenHeader(
                    category = "PROTOTYX · EARNINGS INTELLIGENCE",
                    title = "Earnings Transcripts",
                    subtitle = "AI-extracted executive guidance, corporate remarks, and analyst Q&A"
                )
            }

            // Ticker Input Card
            item(key = "input_card") {
                DoubleBezelCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = "Corporate transcript search",
                            supportingText = "Enter ticker symbol to load executive remarks & analyst Q&A"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TerminalTextField(
                            value = tickerInput,
                            onValueChange = { tickerInput = it },
                            label = "Ticker Symbol",
                            placeholder = "e.g., RELIANCE, TCS, AAPL",
                            leadingIcon = Icons.Default.Search
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        PrimaryButton(
                            text = "Open Earnings Transcript",
                            onClick = {
                                coroutineScope.launch {
                                    if (tickerInput.isBlank()) {
                                        errorMessage = "Please enter a valid ticker symbol."
                                        return@launch
                                    }
                                    isLoading = true
                                    errorMessage = null
                                    transcriptResult = null
                                    try {
                                        transcriptResult = repository.getTranscript(tickerInput.trim().uppercase())
                                    } catch (e: Exception) {
                                        errorMessage = "Transcript retrieval failed: ${e.message ?: "No transcript found for ${tickerInput.uppercase()}"}"
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            isLoading = isLoading,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (errorMessage != null) {
                item(key = "error") {
                    ErrorStateCard(
                        message = errorMessage!!,
                        onRetry = {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    transcriptResult = repository.getTranscript(tickerInput.trim().uppercase())
                                } catch (e: Exception) {
                                    errorMessage = e.message
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    )
                }
            }

            if (isLoading) {
                item(key = "loading") {
                    DoubleBezelCard {
                        TerminalLoadingIndicator(statusText = "Extracting Earnings Intelligence...")
                    }
                }
            }

            transcriptResult?.let { res ->
                // Company & Period Overview
                item(key = "header_info") {
                    DoubleBezelCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = res.companyName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${res.ticker} • Reporting Period: ${res.quarter}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusBadge(text = res.quarter, type = StatusBadgeType.PRIMARY)
                            }

                            res.aiIntelligence?.let { ai ->
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = "AI Takeaways",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "AI STRATEGIC TAKEAWAYS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                ai.strategicTakeaways?.forEach { takeaway ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("•", style = MaterialTheme.typography.bodySmall, color = Mint, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = takeaway,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Prepared Executive Remarks Section
                item(key = "raw_remarks") {
                    DoubleBezelCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(
                                title = "Executive Guidance & Prepared Remarks",
                                supportingText = "Corporate management statements"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = res.preparedRemarks,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }

                // Analyst Q&A Session
                item(key = "qa_content") {
                    DoubleBezelCard {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(
                                title = "Analyst Q&A Session",
                                supportingText = "Institutional questions & management responses"
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = res.qaSession,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EarningsScreenPreview() {
    PrototyxTheme {
        EarningsScreen(repository = MockDataRepository())
    }
}

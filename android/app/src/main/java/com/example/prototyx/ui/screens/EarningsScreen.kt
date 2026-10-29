package com.example.prototyx.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.prototyx.data.DataRepository
import com.example.prototyx.data.MockDataRepository
import com.example.prototyx.data.model.EarningsTranscriptResponse
import com.example.prototyx.theme.PrototyxTheme
import com.example.prototyx.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun EarningsScreen(
    repository: DataRepository,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var tickerInput by remember { mutableStateOf("") }
    var yearInput by remember { mutableStateOf("2026") }
    var quarterInput by remember { mutableStateOf("3") }
    
    var transcriptResult by remember { mutableStateOf<EarningsTranscriptResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(key = "heading") {
            Column {
                EditorialHeading(text = "Earnings")
                Spacer(modifier = Modifier.height(8.dp))
                Text("The source record. Management in their own words.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item(key = "input_card") {
            DoubleBezelCard {
                MetadataLabel(text = "Asset Selection")
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = tickerInput,
                    onValueChange = { tickerInput = it },
                    label = { Text("Ticker (e.g., RELIANCE, TCS)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(yearInput, { yearInput = it }, label = { Text("Fiscal year") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(quarterInput, { quarterInput = it }, label = { Text("Quarter (1–4)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    text = "Open transcript",
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            transcriptResult = null
                            try {
                                val year = yearInput.toIntOrNull()
                                val quarter = quarterInput.toIntOrNull()
                                require(tickerInput.isNotBlank() && year != null && year in 1990..2100 && quarter != null && quarter in 1..4) { "Enter a ticker, fiscal year and quarter from 1 to 4." }
                                transcriptResult = repository.getTranscript(tickerInput.trim().uppercase(), year, quarter)
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "No transcript found for ${tickerInput.uppercase()}"
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

        errorMessage?.let { msg ->
            item(key = "error") {
                StatusMessage(msg, isError = true)
            }
        }

        transcriptResult?.let { res ->
            item(key = "header_info") {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EditorialHeading(text = res.companyName)
                        Spacer(modifier = Modifier.width(8.dp))
                        MetadataLabel(text = "(${res.ticker})")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MetadataLabel(text = "FY Period: ${res.quarter}")
                    }
                }
            }

            // Raw Data Section
            item(key = "raw_remarks_header") {
                MetadataLabel(text = "Full transcript / ROIC.ai")
            }

            item(key = "raw_remarks") {
                DoubleBezelCard {
                    Text(
                        text = res.preparedRemarks,
                        fontSize = 16.sp,
                        lineHeight = 25.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "qa_header") {
                MetadataLabel(text = "Analyst Q&A")
            }

            item(key = "qa_content") {
                DoubleBezelCard {
                    Text(
                        text = res.qaSession,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

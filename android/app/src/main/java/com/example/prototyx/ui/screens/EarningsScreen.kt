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
    
    var transcriptResult by remember { mutableStateOf<EarningsTranscriptResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(key = "heading") {
            Column {
                EditorialHeading(text = "Earnings Intelligence")
                Spacer(modifier = Modifier.height(8.dp))
                MetadataLabel(text = "AI-Driven Corporate Guidance Analysis")
            }
        }

        item(key = "input_card") {
            DoubleBezelCard {
                MetadataLabel(text = "Asset Selection")
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = tickerInput,
                    onValueChange = { tickerInput = it },
                    label = { Text("Ticker (e.g., RELIANCE, TCS)", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Analyze Ingestion Pipeline",
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            transcriptResult = null
                            try {
                                transcriptResult = repository.getTranscript(tickerInput.trim().uppercase())
                            } catch (e: Exception) {
                                errorMessage = "Intelligence Gap: No data found for ${tickerInput.uppercase()}."
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
                Text(msg, color = Color.Red, fontSize = 14.sp)
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
                MetadataLabel(text = "Corporate Guidance (Raw)")
            }

            item(key = "raw_remarks") {
                DoubleBezelCard {
                    Text(
                        text = res.preparedRemarks,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }
            }

            item(key = "qa_header") {
                MetadataLabel(text = "Analyst Q&A Synthesis")
            }

            item(key = "qa_content") {
                DoubleBezelCard {
                    Text(
                        text = res.qaSession,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color.Black.copy(alpha = 0.6f)
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

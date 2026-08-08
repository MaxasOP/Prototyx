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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Earnings Call Ingestion Desk",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Parses corporate transcript dialogues (via earningscall-python) to extract forward growth guidance.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Action inputs card
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
                        value = tickerInput,
                        onValueChange = { tickerInput = it },
                        label = { Text("Stock Ticker (e.g. TCS, RELIANCE, AAPL)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                transcriptResult = null
                                try {
                                    transcriptResult = repository.getTranscript(tickerInput.trim().uppercase())
                                } catch (e: Exception) {
                                    errorMessage = "Failed to fetch earnings transcript. Ensure backend server is running."
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
                            Text("Ingest & Analyze Transcript")
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

        transcriptResult?.let { res ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "${res.companyName} (${res.ticker})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Fiscal Event Period: ${res.quarter}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(text = "Part 1: Prepared Remarks by Executives", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = res.preparedRemarks,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(text = "Part 2: Q&A Session (Analysts v/s Management)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = res.qaSession,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
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

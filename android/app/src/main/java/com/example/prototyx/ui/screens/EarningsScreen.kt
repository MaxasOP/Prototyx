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
    var tickerInput by remember { mutableStateOf("TCS") }
    
    var transcriptResult by remember { mutableStateOf<EarningsTranscriptResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (transcriptResult == null) {
            isLoading = true
            try {
                kotlinx.coroutines.delay(500)
                transcriptResult = repository.getTranscript("TCS")
            } catch (e: Exception) {} finally {
                isLoading = false
            }
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
                EditorialHeading(text = "Earnings Intelligence Ingestion")
                Spacer(modifier = Modifier.height(8.dp))
                MetadataLabel(text = "Natural Language Corporate Guidance Parsing")
            }
        }

        item {
            DoubleBezelCard {
                MetadataLabel(text = "Ingestion Pipeline")
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = tickerInput,
                    onValueChange = { tickerInput = it },
                    label = { Text("Asset Ticker", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.1f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                PrimaryButton(
                    text = "Analyze Fiscal Dialogue",
                    onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            errorMessage = null
                            transcriptResult = null
                            try {
                                transcriptResult = repository.getTranscript(tickerInput.trim().uppercase())
                            } catch (e: Exception) {
                                errorMessage = "Transcript extraction failed. Verify server link."
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

        transcriptResult?.let { res ->
            item {
                DoubleBezelCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EditorialHeading(text = res.companyName)
                        Spacer(modifier = Modifier.width(8.dp))
                        MetadataLabel(text = "(${res.ticker})")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MetadataLabel(text = "Period: ${res.quarter}")
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF1F6C9F), RoundedCornerShape(3.dp)))
                        Spacer(modifier = Modifier.width(4.dp))
                        MetadataLabel(text = "AI PARSED", color = Color(0xFF1F6C9F))
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    MetadataLabel(text = "Strategic Prepared Remarks")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = res.preparedRemarks,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    MetadataLabel(text = "Analyst Q&A Synthesis")
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = res.qaSession,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color.Black.copy(alpha = 0.7f)
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

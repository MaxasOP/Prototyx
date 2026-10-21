package com.example.prototyx.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FinancialDisclaimerDialog(
    onAccept: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* Force explicit user accept */ },
        title = {
            Text(
                text = "⚖️ Financial Disclaimer & Compliance",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Welcome to Prototyx AI Financial Terminal.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "All portfolio optimization models, multi-agent AI debates, risk mesh calculations, and FINNY Virtual Agent recommendations are generated for informational and educational purposes only and do NOT constitute financial, investment, or legal advice.",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Investing in financial markets carries risk of capital loss. Always consult a certified financial advisor before executing real market trades.",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("I Understand & Accept", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    )
}

package com.example.prototyx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.model.NewsSentimentItem
import com.example.prototyx.data.model.UserExperienceMode

@Composable
fun NewsSentimentCard(
    item: NewsSentimentItem,
    modifier: Modifier = Modifier,
    userMode: UserExperienceMode = UserExperienceMode.PRO
) {
    val (badgeBg, badgeText) = when (item.sentiment.uppercase()) {
        "BULLISH" -> MaterialTheme.colorScheme.tertiary to "POSITIVE"
        "BEARISH" -> MaterialTheme.colorScheme.error to "NEGATIVE"
        else -> MaterialTheme.colorScheme.secondary to "NEUTRAL"
    }

    DoubleBezelCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    item.tickers.forEach { ticker ->
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = ticker,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .background(badgeBg.copy(alpha = .12f), CircleShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = badgeBg
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (userMode == UserExperienceMode.SIMPLE) item.plainEnglishSummary else item.aiImpactAnalysis,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${item.source} • ${item.timestamp}",
                fontSize = 10.sp,
                color = Color.Gray
            )
        }
    }
}

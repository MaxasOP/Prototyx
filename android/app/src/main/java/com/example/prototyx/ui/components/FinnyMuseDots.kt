package com.example.prototyx.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.theme.Amber
import com.example.prototyx.theme.Coral
import com.example.prototyx.theme.Mint
import com.example.prototyx.theme.Cyan

enum class FinnyState {
    IDLE, LISTENING, THINKING, SPEAKING, SAFEGUARDING
}

@Composable
fun FinnyMuseDots(
    modifier: Modifier = Modifier,
    state: FinnyState = FinnyState.IDLE,
    dotSize: Dp = 8.dp,
    dotSpacing: Dp = 6.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "muse_dots")

    // Dynamic color depending on FINNY's active state
    val activeColor = when (state) {
        FinnyState.IDLE -> Mint
        FinnyState.LISTENING -> Coral
        FinnyState.THINKING -> Cyan
        FinnyState.SPEAKING -> Mint
        FinnyState.SAFEGUARDING -> Amber
    }

    // 4 Animated Muse Dots with staggered phase shifts
    val scales = (0..3).map { index ->
        val delay = index * 150
        infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = if (state != FinnyState.IDLE) 1.35f else 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(600, delayMillis = delay, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_scale_$index"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dotSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        scales.forEach { scale ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(scale.value)
                    .background(activeColor, CircleShape)
            )
        }
    }
}

@Composable
fun FinnyVirtualAgentCard(
    healthScore: Int,
    briefingSummary: String,
    onVoiceTap: () -> Unit,
    modifier: Modifier = Modifier,
    state: FinnyState = FinnyState.IDLE
) {
    DoubleBezelCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingSignal(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "DAILY BRIEF",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        color = when {
                            healthScore >= 80 -> Mint.copy(alpha = .12f)
                            healthScore >= 60 -> Amber.copy(alpha = .12f)
                            else -> Coral.copy(alpha = .12f)
                        },
                        shape = CircleShape
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "PORTFOLIO HEALTH $healthScore%",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        healthScore >= 80 -> Mint
                        healthScore >= 60 -> Amber
                        else -> Coral
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = briefingSummary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        PrimaryButton(
            text = "Ask about this brief",
            onClick = onVoiceTap,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

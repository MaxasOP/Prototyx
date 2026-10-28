package com.example.prototyx.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.prototyx.theme.Hairline
import com.example.prototyx.theme.Mint
import kotlin.math.sin

/** A quiet, data-inspired brand graphic. One slow animation; no particles or blur. */
@Composable
fun MarketAtmosphere(
    modifier: Modifier = Modifier,
    accent: Color = Mint
) {
    val transition = rememberInfiniteTransition(label = "market_atmosphere")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "market_phase"
    )

    Canvas(modifier) {
        val columns = 6
        val rows = 3
        repeat(columns + 1) { i ->
            val x = size.width * i / columns
            drawLine(Hairline.copy(alpha = .34f), Offset(x, 0f), Offset(x, size.height), 1f)
        }
        repeat(rows + 1) { i ->
            val y = size.height * i / rows
            drawLine(Hairline.copy(alpha = .28f), Offset(0f, y), Offset(size.width, y), 1f)
        }

        val path = Path()
        val points = 36
        repeat(points) { i ->
            val x = size.width * i / (points - 1)
            val trend = size.height * (.72f - i * .008f)
            val wave = sin((i * .62f) + phase * 6.283f) * size.height * .075f
            val y = trend + wave
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            brush = Brush.horizontalGradient(listOf(accent.copy(alpha = .18f), accent, accent.copy(alpha = .4f))),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        val pulseX = size.width * phase
        drawCircle(accent.copy(alpha = .13f), 12.dp.toPx(), Offset(pulseX, size.height * .5f))
        drawCircle(accent, 2.5.dp.toPx(), Offset(pulseX, size.height * .5f))
    }
}

@Composable
fun AnimatedScoreRing(
    score: Int,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val progress by animateFloatAsState(
        targetValue = score.coerceIn(0, 100) / 100f,
        animationSpec = spring(dampingRatio = .82f, stiffness = Spring.StiffnessLow),
        label = "score_progress"
    )
    Box(modifier.size(size)) {
        Canvas(Modifier.fillMaxSize()) {
            val width = 5.dp.toPx()
            val inset = width / 2
            drawArc(
                color = Hairline,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - width, this.size.height - width),
                style = Stroke(width, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(this.size.width - width, this.size.height - width),
                style = Stroke(width, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun PulsingSignal(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "signal")
    val alpha by transition.animateFloat(
        .16f, .03f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "signal_alpha"
    )
    Canvas(modifier.size(18.dp)) {
        drawCircle(color.copy(alpha = alpha), radius = size.minDimension / 2)
        drawCircle(color, radius = 3.dp.toPx())
    }
}

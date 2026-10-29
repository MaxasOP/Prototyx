package com.example.prototyx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prototyx.data.model.PricePoint

@Composable
fun StockChartView(
    ticker: String,
    pricePoints: List<PricePoint>,
    modifier: Modifier = Modifier,
    isCandleStyle: Boolean = false,
    selectedPeriod: String = "1M",
    onPeriodChanged: (String) -> Unit = {}
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var chartVisible by remember(pricePoints) { mutableStateOf(false) }
    LaunchedEffect(pricePoints) { chartVisible = true }
    val revealProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (chartVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 220,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "chart_reveal"
    )

    val periods = listOf("1D", "1W", "1M", "1Y", "5Y")

    DoubleBezelCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(ticker, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                MetadataLabel("ADJUSTED PRICE HISTORY")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                periods.forEach { p ->
                    TextButton(onClick = { selectedPointIndex = null; onPeriodChanged(p) },
                        colors = ButtonDefaults.textButtonColors(contentColor = if (selectedPeriod == p) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)) {
                        Text(p, fontSize = 12.sp, fontWeight = if (selectedPeriod == p) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))
            // Canvas Area
            if (pricePoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                val activePoint = selectedPointIndex?.let { pricePoints.getOrNull(it) } ?: pricePoints.last()

                // Inspected price overlay
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format("%.2f", activePoint.close),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    activePoint.rsi?.let { rsiVal ->
                        Text(
                            text = "RSI: ${rsiVal.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (rsiVal > 70) Color.Red else if (rsiVal < 30) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(activePoint.timestamp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    val lineColor = MaterialTheme.colorScheme.primary
                    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pricePoints) {
                                detectTapGestures { offset ->
                                    if (pricePoints.isEmpty()) return@detectTapGestures
                                    val pointWidth = size.width / (pricePoints.size - 1).coerceAtLeast(1)
                                    val index = (offset.x / pointWidth).toInt().coerceIn(0, pricePoints.size - 1)
                                    selectedPointIndex = index
                                }
                            }
                    ) {
                        if (pricePoints.isEmpty()) return@Canvas
                        val width = size.width
                        val height = size.height

                        val minPrice = pricePoints.minOf { it.close }
                        val maxPrice = pricePoints.maxOf { it.close }
                        val priceRange = (maxPrice - minPrice).coerceAtLeast(1f)

                        // Draw Grid Lines
                        val gridLines = 4
                        for (i in 0..gridLines) {
                            val y = height * (i.toFloat() / gridLines)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Draw Price Line
                        val path = Path()
                        val stepX = width / (pricePoints.size - 1).coerceAtLeast(1)

                        val revealedCount = (pricePoints.size * revealProgress).toInt().coerceIn(1, pricePoints.size)
                        pricePoints.take(revealedCount).forEachIndexed { index, point ->
                            val x = index * stepX
                            val y = height - ((point.close - minPrice) / priceRange * height * 0.85f) - (height * 0.05f)

                            if (index == 0) {
                                path.moveTo(x, y)
                            } else {
                                path.lineTo(x, y)
                            }
                        }


                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Draw Active Point Marker
                        selectedPointIndex?.let { idx ->
                            if (idx in pricePoints.indices) {
                                val pt = pricePoints[idx]
                                val x = idx * stepX
                                val y = height - ((pt.close - minPrice) / priceRange * height * 0.85f) - (height * 0.05f)

                                drawLine(
                                    color = Color.Gray,
                                    start = Offset(x, 0f),
                                    end = Offset(x, height),
                                    strokeWidth = 1.dp.toPx()
                                )

                                drawCircle(
                                    color = lineColor,
                                    radius = 6.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
    isCandleStyle: Boolean = false
) {
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var selectedPeriod by remember { mutableStateOf("1M") }

    val periods = listOf("1D", "1W", "1M", "1Y", "5Y")

    DoubleBezelCard(modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with Price & Ticker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$ticker Interactive Chart",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    MetadataLabel(text = "TECHNICAL PRICE & VOLUME MATRIX")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    periods.forEach { p ->
                        FilterChip(
                            selected = (selectedPeriod == p),
                            onClick = { selectedPeriod = p },
                            label = { Text(p, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        text = "₹${String.format("%.2f", activePoint.close)}",
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
                                    val pointWidth = size.width / (pricePoints.size - 1).coerceAtLeast(1)
                                    val index = (offset.x / pointWidth).toInt().coerceIn(0, pricePoints.size - 1)
                                    selectedPointIndex = index
                                }
                            }
                    ) {
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

                        pricePoints.forEachIndexed { index, point ->
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
                            style = Stroke(width = 3.dp.toPx())
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

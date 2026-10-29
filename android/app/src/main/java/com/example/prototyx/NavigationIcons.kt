package com.example.prototyx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val FinnyAssistantIcon = ImageVector.Builder(
    name = "FinnyAssistant", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f
).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(7f, 3f); lineTo(17f, 3f)
        curveTo(20f, 3f, 21f, 5f, 21f, 8f); lineTo(21f, 14f)
        curveTo(21f, 17f, 19f, 19f, 16f, 19f)
        lineTo(10f, 19f); lineTo(5f, 22f); lineTo(5f, 18f)
        curveTo(3f, 17f, 3f, 16f, 3f, 14f); lineTo(3f, 8f)
        curveTo(3f, 5f, 4f, 3f, 7f, 3f); close()
        moveTo(8f, 9f); lineTo(8f, 11f)
        moveTo(16f, 9f); lineTo(16f, 11f)
        moveTo(9f, 14f); curveTo(11f, 16f, 13f, 16f, 15f, 14f)
    }
}.build()

// Match the weight and bounds of the outlined navigation icon family.
internal val ResearchNavigationIcon: ImageVector = ImageVector.Builder(
    name = "ResearchReport", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f
).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(14f, 3f); lineTo(5f, 3f); lineTo(5f, 21f)
        lineTo(19f, 21f); lineTo(19f, 8f); close()
        moveTo(14f, 3f); lineTo(14f, 8f); lineTo(19f, 8f)
        moveTo(9f, 12f); lineTo(15f, 12f)
        moveTo(9f, 16f); lineTo(15f, 16f)
    }
}.build()

internal val EarningsNavigationIcon: ImageVector = ImageVector.Builder(
    name = "EarningsResults", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f
).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        // Quarterly results: a report frame containing three financial bars.
        moveTo(5f, 3f); lineTo(19f, 3f); lineTo(21f, 5f)
        lineTo(21f, 19f); lineTo(19f, 21f); lineTo(5f, 21f)
        lineTo(3f, 19f); lineTo(3f, 5f); close()
        moveTo(7f, 17f); lineTo(7f, 13f)
        moveTo(12f, 17f); lineTo(12f, 7f)
        moveTo(17f, 17f); lineTo(17f, 10f)
    }
}.build()

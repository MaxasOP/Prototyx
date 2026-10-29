package com.example.prototyx

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

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

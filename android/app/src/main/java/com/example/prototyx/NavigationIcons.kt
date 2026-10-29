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
    name = "EarningsCall", defaultWidth = 24.dp, defaultHeight = 24.dp,
    viewportWidth = 24f, viewportHeight = 24f
).apply {
    path(stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
        moveTo(9f, 5f)
        curveTo(9f, 1f, 15f, 1f, 15f, 5f)
        lineTo(15f, 11f)
        curveTo(15f, 15f, 9f, 15f, 9f, 11f); close()
        moveTo(5f, 10f); lineTo(5f, 11f)
        curveTo(5f, 20f, 19f, 20f, 19f, 11f); lineTo(19f, 10f)
        moveTo(12f, 18f); lineTo(12f, 22f)
        moveTo(9f, 22f); lineTo(15f, 22f)
    }
}.build()

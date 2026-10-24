package com.example.prototyx.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val PrototyxSans = FontFamily.SansSerif

val Typography = Typography(
    displayLarge = TextStyle(fontFamily = PrototyxSans, fontWeight = FontWeight.SemiBold, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-1.8).sp),
    displayMedium = TextStyle(fontFamily = PrototyxSans, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, lineHeight = 42.sp, letterSpacing = (-1.2).sp),
    headlineLarge = TextStyle(fontFamily = PrototyxSans, fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontFamily = PrototyxSans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontFamily = PrototyxSans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.25).sp),
    titleMedium = TextStyle(fontFamily = PrototyxSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = PrototyxSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = PrototyxSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = PrototyxSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = PrototyxSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontFamily = PrototyxSans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.7.sp),
    labelSmall = TextStyle(fontFamily = PrototyxSans, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.sp)
)

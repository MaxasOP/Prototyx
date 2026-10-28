package com.example.prototyx.theme

import androidx.compose.ui.graphics.Color

// Prototyx Design Tokens — Restrained Institutional Financial Terminal Palette
val Ink = Color(0xFF0B0F0E)            // Main Background (#0B0F0E)
val InkElevated = Color(0xFF0F1413)    // Surface Base
val Graphite = Color(0xFF131917)       // Primary Surface (#131917)
val GraphiteSoft = Color(0xFF1A211E)   // Secondary Surface (#1A211E)
val Hairline = Color(0xFF222B26)       // Muted Divider
val HairlineBright = Color(0xFF2A332E) // Borders (#2A332E)
val Cloud = Color(0xFFE7ECE8)          // Primary Text (#E7ECE8)
val Mist = Color(0xFF9AA69F)           // Secondary Text (#9AA69F)
val Dim = Color(0xFF63706A)            // Muted / Disabled Text
val Signal = Color(0xFF287856)         // Primary Accent Dark
val SignalBright = Color(0xFF59B88A)   // Primary Accent / Mint (#59B88A)
val Cyan = Color(0xFF88B3A6)           // Secondary Accent
val Mint = Color(0xFF59B88A)           // Positive Performance (#59B88A)
val Amber = Color(0xFFD5AC65)          // Warning (#D5AC65)
val Coral = Color(0xFFD87575)          // Negative Performance (#D87575)

// Semantic aliases retained for existing feature code compatibility.
val BackgroundWarm = Ink
val SurfaceWhite = Graphite
val TextCharcoal = Cloud
val TextMuted = Mist
val BorderLight = HairlineBright
val PaleRed = Color(0xFF2A171C)
val PaleRedText = Coral
val PaleBlue = Color(0xFF14232D)
val PaleBlueText = Cyan
val PaleGreen = Color(0xFF13251F)
val PaleGreenText = Mint
val PaleYellow = Color(0xFF2A2316)
val PaleYellowText = Amber

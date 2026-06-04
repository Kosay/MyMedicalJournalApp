package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Brand colors matching the screenshots (minimalist dark slate)
val SlateDarkBg = Color(0xFF0E1624)       // Screen background
val SlateCardBg = Color(0xFF192537)       // Card color
val ActiveTabBg = Color(0xFF283A4E)       // Selected bottom navigation item background
val HighlightTeal = Color(0xFF3CD3AD)     // Teal accent (water water, saves, etc.)
val WarningYellow = Color(0xFFF5C024)     // Elevated indicators (e.g. elevated BP)
val AlertRed = Color(0xFFEA5B5B)           // Warning red (e.g. triglycerides > 150)

// Normal Material scheme fallback tokens
val PrimaryDark = HighlightTeal
val SecondaryDark = ActiveTabBg
val TertiaryDark = WarningYellow

val PrimaryLight = Color(0xFF006C50)
val SecondaryLight = Color(0xFF4C6359)
val TertiaryLight = Color(0xFF825500)
val LightBg = Color(0xFFF4F9F6)
val LightCardBg = Color(0xFFFFFFFF)

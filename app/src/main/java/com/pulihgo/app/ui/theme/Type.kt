package com.pulihgo.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pulihgo.app.R

/**
 * PulihGo's single font family, covering Patient and future Therapist UI.
 * Run tools/install-plus-jakarta-sans.ps1 once to populate res/font.
 * Font source: Tokotype / Plus Jakarta Sans (SIL Open Font License 1.1).
 */
val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold)
)

private fun pulihStyle(
    size: Int,
    line: Int,
    weight: FontWeight = FontWeight.Normal,
    tracking: Float = 0f
) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

/**
 * Shared typography tokens (sp). Screens may override size/weight when a
 * particular Figma component needs it; the font family remains consistent.
 */
val Typography = Typography(
    displayLarge = pulihStyle(48, 56, FontWeight.ExtraBold),
    displayMedium = pulihStyle(40, 48, FontWeight.ExtraBold),
    displaySmall = pulihStyle(34, 42, FontWeight.Bold),
    headlineLarge = pulihStyle(28, 36, FontWeight.ExtraBold),
    headlineMedium = pulihStyle(24, 32, FontWeight.Bold),
    headlineSmall = pulihStyle(22, 30, FontWeight.Bold),
    titleLarge = pulihStyle(20, 28, FontWeight.Bold),
    titleMedium = pulihStyle(16, 24, FontWeight.SemiBold),
    titleSmall = pulihStyle(14, 20, FontWeight.SemiBold),
    bodyLarge = pulihStyle(16, 24),
    bodyMedium = pulihStyle(14, 22),
    bodySmall = pulihStyle(12, 18),
    labelLarge = pulihStyle(16, 22, FontWeight.Bold),
    labelMedium = pulihStyle(12, 18, FontWeight.SemiBold),
    labelSmall = pulihStyle(11, 16, FontWeight.Medium)
)

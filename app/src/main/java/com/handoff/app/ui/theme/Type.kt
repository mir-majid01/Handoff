package com.handoff.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.handoff.app.R

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

// Bundled soft, warm serif (Spectral, OFL) used for the wordmark and screen titles.
val SerifFamily = FontFamily(
    Font(R.font.spectral_regular, FontWeight.Normal),
    Font(R.font.spectral_medium, FontWeight.Medium),
    Font(R.font.spectral_semibold, FontWeight.SemiBold),
    Font(R.font.spectral_bold, FontWeight.Bold),
)

private val Inter = TextStyle(fontFamily = InterFamily)
private val Serif = TextStyle(fontFamily = SerifFamily)

/** The "Handoff" brand lock-up: serif SemiBold, tight tracking. */
val WordmarkStyle = Serif.copy(
    fontWeight = FontWeight.SemiBold,
    fontSize = 30.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.5).sp,
)

/** Serif page titles in the 56dp bars and large headers. */
val ScreenTitleStyle = Serif.copy(
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
    lineHeight = 32.sp,
    letterSpacing = (-0.3).sp,
)

/** Compact serif title for the top bar (fits 56dp without clipping at large font scale). */
val BarTitleStyle = Serif.copy(
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.2).sp,
)

/** Uppercase section label: Inter SemiBold 13sp, +0.8 tracking. Call sites pass the uppercased text. */
val SectionHeaderStyle = Inter.copy(
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.8.sp,
)

/** Home tagline under the wordmark: Inter Medium 14sp. */
val TaglineStyle = Inter.copy(
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 18.sp,
)

/**
 * iOS-flavoured scale on Inter: large titles are tight and bold, sections at 20sp
 * semibold, body at 16sp, captions at 13sp. Line heights follow Inter's x-height
 * for comfortable reading at 200% font scale.
 */
val HandoffTypography = Typography(
    displayLarge = Inter.copy(fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 41.sp, letterSpacing = (-0.3).sp),
    displayMedium = Inter.copy(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.3).sp),
    displaySmall = Inter.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.2).sp),
    headlineLarge = Inter.copy(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.2).sp),
    headlineMedium = Inter.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    headlineSmall = Inter.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp, letterSpacing = (-0.1).sp),
    titleLarge = Inter.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp, letterSpacing = (-0.1).sp),
    titleMedium = Inter.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    titleSmall = Inter.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = Inter.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = Inter.copy(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 20.sp),
    bodySmall = Inter.copy(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = Inter.copy(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp),
    labelMedium = Inter.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
    labelSmall = Inter.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.2.sp),
)

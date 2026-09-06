package com.familymoney.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.familymoney.R

/**
 * Heebo (spec section 51), bundled rather than fetched so Hebrew renders
 * identically on every device.
 *
 * OptionalLocal means a font resource that fails to parse degrades to the system
 * typeface instead of throwing — text stays readable no matter what.
 */
val Heebo = FontFamily(
    Font(R.font.heebo_regular, FontWeight.Normal, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.heebo_medium, FontWeight.Medium, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.heebo_semibold, FontWeight.SemiBold, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.heebo_bold, FontWeight.Bold, loadingStrategy = FontLoadingStrategy.OptionalLocal),
    Font(R.font.heebo_extrabold, FontWeight.ExtraBold, loadingStrategy = FontLoadingStrategy.OptionalLocal)
)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp, lineHeight = 52.sp),
    displayMedium = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    displaySmall = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp),

    headlineLarge = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),

    titleLarge = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),

    bodyLarge = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),

    labelLarge = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)

/** Numbers get their own weight so amounts read as amounts (spec section 51). */
val MoneyLarge = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 48.sp)
val MoneyMedium = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp)
val MoneySmall = TextStyle(fontFamily = Heebo, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp)

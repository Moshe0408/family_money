package com.familymoney.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = PrimaryDark,
    secondary = PurpleAccent,
    onSecondary = Color.White,
    secondaryContainer = PurpleLight,
    onSecondaryContainer = Color(0xFF4C1D95),
    tertiary = Success,
    onTertiary = Color.White,
    tertiaryContainer = SuccessLight,
    onTertiaryContainer = Color(0xFF065F46),
    error = Danger,
    onError = Color.White,
    errorContainer = DangerLight,
    onErrorContainer = Color(0xFF991B1B),
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = CardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    outlineVariant = Color(0xFFEEF2F7),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE9EEF5),
    surfaceContainerLow = Color(0xFFFBFCFE)
)

private val DarkColors = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color(0xFF0B1120),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = PurpleLight,
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF042F26),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = SuccessLight,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = DangerLight,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = CardDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF232E45),
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    outlineVariant = Color(0xFF243049),
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = Color(0xFF202B41),
    surfaceContainerLow = Color(0xFF0F172A)
)

/** Radius 16-20 per spec section 51. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val LocalIsDark = staticCompositionLocalOf { false }

@Composable
fun FamilyMoneyTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalContext.current

    SideEffect {
        (view as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView)
                .isAppearanceLightStatusBars = !dark
        }
    }

    // Section 51: the whole product is right-to-left.
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalIsDark provides dark
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

package com.familymoney.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/** Radius 16-20 per spec section 51. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val LocalIsDark = staticCompositionLocalOf { true }

/**
 * Exposes the raw palette alongside the Material scheme. Screens use this for
 * the semantic colours Material has no slot for — positive, negative, hero.
 */
val LocalPalette = staticCompositionLocalOf { AppPalette.MIDNIGHT.colors() }

@Composable
fun FamilyMoneyTheme(
    paletteId: String = AppPalette.MIDNIGHT.id,
    content: @Composable () -> Unit
) {
    val palette = AppPalette.fromId(paletteId)
    val c = palette.colors()

    val scheme = if (palette.dark) {
        darkColorScheme(
            primary = c.accent,
            onPrimary = c.onAccent,
            primaryContainer = c.accentSoft,
            onPrimaryContainer = c.accent,
            secondary = c.info,
            onSecondary = c.background,
            secondaryContainer = c.surfaceElevated,
            onSecondaryContainer = c.text,
            tertiary = c.positive,
            onTertiary = c.background,
            tertiaryContainer = c.accentSoft,
            onTertiaryContainer = c.positive,
            error = c.negative,
            onError = Color.White,
            errorContainer = c.negative.copy(alpha = 0.16f),
            onErrorContainer = c.negative,
            background = c.background,
            onBackground = c.text,
            surface = c.surface,
            onSurface = c.text,
            surfaceVariant = c.surfaceElevated,
            onSurfaceVariant = c.textMuted,
            outline = c.outline,
            outlineVariant = c.outlineSoft,
            surfaceContainer = c.surfaceContainer,
            surfaceContainerHigh = c.surfaceElevated,
            surfaceContainerLow = c.background,
            inverseSurface = c.surfaceElevated,
            inverseOnSurface = c.text
        )
    } else {
        lightColorScheme(
            primary = c.accent,
            onPrimary = c.onAccent,
            primaryContainer = c.accentSoft,
            onPrimaryContainer = c.accent,
            secondary = c.info,
            onSecondary = Color.White,
            secondaryContainer = c.accentSoft,
            onSecondaryContainer = c.accent,
            tertiary = c.positive,
            onTertiary = Color.White,
            tertiaryContainer = c.positive.copy(alpha = 0.14f),
            onTertiaryContainer = c.positive,
            error = c.negative,
            onError = Color.White,
            errorContainer = c.negative.copy(alpha = 0.12f),
            onErrorContainer = c.negative,
            background = c.background,
            onBackground = c.text,
            surface = c.surface,
            onSurface = c.text,
            surfaceVariant = c.surfaceContainer,
            onSurfaceVariant = c.textMuted,
            outline = c.outline,
            outlineVariant = c.outlineSoft,
            surfaceContainer = c.surfaceContainer,
            surfaceContainerHigh = c.outlineSoft,
            surfaceContainerLow = c.surface,
            inverseSurface = c.text,
            inverseOnSurface = c.background
        )
    }

    val context = LocalContext.current
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            window.statusBarColor = c.background.toArgb()
            window.navigationBarColor = c.background.toArgb()
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !palette.dark
                isAppearanceLightNavigationBars = !palette.dark
            }
        }
    }

    // Section 51: the whole product is right-to-left.
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalIsDark provides palette.dark,
        LocalPalette provides c
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

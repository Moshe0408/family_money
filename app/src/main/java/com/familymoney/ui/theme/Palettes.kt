package com.familymoney.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Selectable colour schemes.
 *
 * Money apps live or die on how trustworthy they look, so each palette is a
 * complete, self-consistent set rather than a hue shift of one base theme.
 */
enum class AppPalette(
    val id: String,
    val he: String,
    val description: String,
    val dark: Boolean,
    val swatch: List<Color>
) {
    /** The default: deep navy with emerald accents, à la modern fintech. */
    MIDNIGHT(
        id = "midnight",
        he = "חצות",
        description = "כחול לילה עם ירוק אמרלד",
        dark = true,
        swatch = listOf(Color(0xFF0C141D), Color(0xFF34D399), Color(0xFF1B2734))
    ),

    /** Near-black with a warm gold accent — quieter, more formal. */
    OBSIDIAN(
        id = "obsidian",
        he = "אובסידיאן",
        description = "שחור עם זהב",
        dark = true,
        swatch = listOf(Color(0xFF0A0A0C), Color(0xFFE0B44C), Color(0xFF16161A))
    ),

    /** Cool indigo, closer to the original brief in section 51. */
    INDIGO(
        id = "indigo",
        he = "אינדיגו",
        description = "כחול עמוק עם סגול",
        dark = true,
        swatch = listOf(Color(0xFF0B1020), Color(0xFF818CF8), Color(0xFF161C33))
    ),

    /** The light scheme from the original design system. */
    DAYLIGHT(
        id = "daylight",
        he = "בהיר",
        description = "לבן עם כחול",
        dark = false,
        swatch = listOf(Color(0xFFF8FAFC), Color(0xFF2563EB), Color(0xFFFFFFFF))
    ),

    /** Warm light scheme — paper-like, easy on the eyes. */
    LINEN(
        id = "linen",
        he = "פשתן",
        description = "קרם חמים עם ירוק",
        dark = false,
        swatch = listOf(Color(0xFFF6F4EF), Color(0xFF0F766E), Color(0xFFFFFDF8))
    );

    companion object {
        fun fromId(id: String?): AppPalette =
            entries.firstOrNull { it.id == id } ?: MIDNIGHT
    }
}

/** Every colour a screen needs, resolved for one palette. */
data class PaletteColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceContainer: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val text: Color,
    val textMuted: Color,
    val outline: Color,
    val outlineSoft: Color,
    val positive: Color,
    val negative: Color,
    val warning: Color,
    val info: Color,
    val heroStart: Color,
    val heroMid: Color,
    val heroEnd: Color
)

fun AppPalette.colors(): PaletteColors = when (this) {

    AppPalette.MIDNIGHT -> PaletteColors(
        background = Color(0xFF0C141D),
        surface = Color(0xFF16202C),
        surfaceElevated = Color(0xFF1B2734),
        surfaceContainer = Color(0xFF121B25),
        accent = Color(0xFF34D399),
        accentSoft = Color(0xFF10362C),
        onAccent = Color(0xFF04231A),
        text = Color(0xFFE8EDF2),
        textMuted = Color(0xFF8A97A6),
        outline = Color(0xFF243141),
        outlineSoft = Color(0xFF1B2734),
        positive = Color(0xFF34D399),
        negative = Color(0xFFF56565),
        warning = Color(0xFFF6AD55),
        info = Color(0xFF63B3ED),
        heroStart = Color(0xFF16202C),
        heroMid = Color(0xFF13202A),
        heroEnd = Color(0xFF102A26)
    )

    AppPalette.OBSIDIAN -> PaletteColors(
        background = Color(0xFF0A0A0C),
        surface = Color(0xFF16161A),
        surfaceElevated = Color(0xFF1D1D22),
        surfaceContainer = Color(0xFF111114),
        accent = Color(0xFFE0B44C),
        accentSoft = Color(0xFF382D13),
        onAccent = Color(0xFF241B05),
        text = Color(0xFFF0EFEC),
        textMuted = Color(0xFF97948D),
        outline = Color(0xFF2A2A30),
        outlineSoft = Color(0xFF1D1D22),
        positive = Color(0xFF6EE7A8),
        negative = Color(0xFFF77272),
        warning = Color(0xFFE0B44C),
        info = Color(0xFF8FB8E8),
        heroStart = Color(0xFF1A1A1F),
        heroMid = Color(0xFF201D18),
        heroEnd = Color(0xFF2A2314)
    )

    AppPalette.INDIGO -> PaletteColors(
        background = Color(0xFF0B1020),
        surface = Color(0xFF161C33),
        surfaceElevated = Color(0xFF1D2440),
        surfaceContainer = Color(0xFF11162A),
        accent = Color(0xFF818CF8),
        accentSoft = Color(0xFF262E5C),
        onAccent = Color(0xFF0B1020),
        text = Color(0xFFE7E9F5),
        textMuted = Color(0xFF8E94B8),
        outline = Color(0xFF262D4A),
        outlineSoft = Color(0xFF1D2440),
        positive = Color(0xFF4ADE80),
        negative = Color(0xFFFB7185),
        warning = Color(0xFFFBBF24),
        info = Color(0xFF818CF8),
        heroStart = Color(0xFF1E2450),
        heroMid = Color(0xFF2A2A63),
        heroEnd = Color(0xFF3B2E6B)
    )

    AppPalette.DAYLIGHT -> PaletteColors(
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        surfaceElevated = Color(0xFFFFFFFF),
        surfaceContainer = Color(0xFFF1F5F9),
        accent = Color(0xFF2563EB),
        accentSoft = Color(0xFFDBEAFE),
        onAccent = Color(0xFFFFFFFF),
        text = Color(0xFF111827),
        textMuted = Color(0xFF6B7280),
        outline = Color(0xFFE5E7EB),
        outlineSoft = Color(0xFFEEF2F7),
        positive = Color(0xFF10B981),
        negative = Color(0xFFEF4444),
        warning = Color(0xFFF59E0B),
        info = Color(0xFF2563EB),
        heroStart = Color(0xFF2563EB),
        heroMid = Color(0xFF4F46E5),
        heroEnd = Color(0xFF7C3AED)
    )

    AppPalette.LINEN -> PaletteColors(
        background = Color(0xFFF6F4EF),
        surface = Color(0xFFFFFDF8),
        surfaceElevated = Color(0xFFFFFFFF),
        surfaceContainer = Color(0xFFEFEDE6),
        accent = Color(0xFF0F766E),
        accentSoft = Color(0xFFD3EDE9),
        onAccent = Color(0xFFFFFFFF),
        text = Color(0xFF1C1917),
        textMuted = Color(0xFF78716C),
        outline = Color(0xFFE2DED4),
        outlineSoft = Color(0xFFEDEAE2),
        positive = Color(0xFF0F766E),
        negative = Color(0xFFDC2626),
        warning = Color(0xFFCA8A04),
        info = Color(0xFF0369A1),
        heroStart = Color(0xFF0F766E),
        heroMid = Color(0xFF115E59),
        heroEnd = Color(0xFF134E4A)
    )
}

package com.notey.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.pow

/**
 * SECTION 2: 5-Tier Surface Elevation Architecture
 */
object NoteySurfaces {
    val Surface0 = Color(0xFF000000) // Pure OLED Black (Base Canvas / Background)
    val Surface1 = Color(0xFF09090B) // Zinc 950 (Base Cards / Sidebars)
    val Surface2 = Color(0xFF121215) // Zinc 900 (Modals / Bottom Sheets / Floating Bars)
    val Surface3 = Color(0xFF18181B) // Zinc 850 (Active Inputs / Dock / Hover)
    val Surface4 = Color(0xFF27272A) // Zinc 800 (Chips / Pills / Hairline Borders)
    val HairlineBorder = Color(0x14FFFFFF) // 1px solid rgba(255, 255, 255, 0.08)
    val HairlineBorderStrong = Color(0x28FFFFFF) // 1px solid rgba(255, 255, 255, 0.16)

    val TextPrimary = Color(0xFFFFFFFF) // Crisp White
    val TextSecondary = Color(0xFFA1A1AA) // Zinc 400
    val TextMuted = Color(0xFF71717A) // Zinc 500
    val AccentLine = Color(0xFF3F3F46) // Zinc 700
}

/**
 * Automated WCAG 2.1 relative luminance calculation:
 * Y = 0.2126 * R_lin + 0.7152 * G_lin + 0.0722 * B_lin
 */
fun calculateRelativeLuminance(color: Color): Float {
    fun channel(c: Float): Float {
        return if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }
    val r = channel(color.red)
    val g = channel(color.green)
    val b = channel(color.blue)
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

/**
 * Dynamic Inversion System:
 * - If Y > 0.42 -> Invert to #000000 (Deep Obsidian)
 * - If Y <= 0.42 -> Invert to #FFFFFF (Crisp White)
 */
fun getInvertedTextColor(backgroundTint: Color): Color {
    val y = calculateRelativeLuminance(backgroundTint)
    return if (y > 0.42f) Color(0xFF000000) else Color(0xFFFFFFFF)
}

val NoteAccentColors = listOf(
    Color(0xFF38BDF8), // Electric Slate
    Color(0xFF34D399), // Nordic Mint
    Color(0xFFFBBF24), // Solar Amber
    Color(0xFFA78BFA), // Hyper Violet
    Color(0xFFFB7185), // Crimson Pulse
    Color(0xFFF4F4F5), // Monochrome Pure
    Color(0xFF818CF8), // Indigo
    Color(0xFF94A3B8)  // Slate
)

fun getNoteColor(index: Int): Color {
    val i = (index % NoteAccentColors.size + NoteAccentColors.size) % NoteAccentColors.size
    return NoteAccentColors[i]
}

fun createAmoledColorScheme(preset: ColorPreset): ColorScheme {
    return darkColorScheme(
        primary = preset.primary,
        onPrimary = getInvertedTextColor(preset.primary),
        primaryContainer = NoteySurfaces.Surface3,
        onPrimaryContainer = preset.primary,
        secondary = preset.secondary,
        onSecondary = getInvertedTextColor(preset.secondary),
        secondaryContainer = NoteySurfaces.Surface2,
        onSecondaryContainer = NoteySurfaces.TextPrimary,
        tertiary = preset.tertiary,
        onTertiary = Color.Black,
        background = NoteySurfaces.Surface0,
        onBackground = NoteySurfaces.TextPrimary,
        surface = NoteySurfaces.Surface1,
        onSurface = NoteySurfaces.TextPrimary,
        surfaceVariant = NoteySurfaces.Surface2,
        onSurfaceVariant = NoteySurfaces.TextSecondary,
        outline = NoteySurfaces.HairlineBorder,
        outlineVariant = NoteySurfaces.Surface4,
    )
}

fun createZincColorScheme(preset: ColorPreset): ColorScheme {
    return darkColorScheme(
        primary = preset.primary,
        onPrimary = getInvertedTextColor(preset.primary),
        primaryContainer = Color(0xFF1E1E24),
        onPrimaryContainer = preset.primary,
        secondary = preset.secondary,
        onSecondary = getInvertedTextColor(preset.secondary),
        secondaryContainer = Color(0xFF16161A),
        onSecondaryContainer = NoteySurfaces.TextPrimary,
        tertiary = preset.tertiary,
        onTertiary = Color.Black,
        background = Color(0xFF09090B),
        onBackground = NoteySurfaces.TextPrimary,
        surface = Color(0xFF121215),
        onSurface = NoteySurfaces.TextPrimary,
        surfaceVariant = Color(0xFF18181B),
        onSurfaceVariant = NoteySurfaces.TextSecondary,
        outline = NoteySurfaces.HairlineBorder,
        outlineVariant = NoteySurfaces.Surface4,
    )
}

fun createDynamicColorScheme(preset: ColorPreset): ColorScheme {
    return createAmoledColorScheme(preset)
}

fun createLiquidGlassColorScheme(preset: ColorPreset): ColorScheme {
    return darkColorScheme(
        primary = preset.primary,
        onPrimary = getInvertedTextColor(preset.primary),
        primaryContainer = Color(0x3338BDF8),
        onPrimaryContainer = preset.primary,
        secondary = preset.secondary,
        onSecondary = getInvertedTextColor(preset.secondary),
        secondaryContainer = Color(0x2E1E293B),
        onSecondaryContainer = NoteySurfaces.TextPrimary,
        tertiary = preset.tertiary,
        onTertiary = Color.Black,
        background = Color(0xFF030712), // Deep Void Midnight
        onBackground = NoteySurfaces.TextPrimary,
        surface = Color(0xFF0D1424), // Acrylic Translucent Glass Layer
        onSurface = NoteySurfaces.TextPrimary,
        surfaceVariant = Color(0xFF131D33), // Refractive Glass Surface
        onSurfaceVariant = Color(0xFF94A3B8),
        outline = Color(0x3338BDF8), // Specular Iridescent Border
        outlineVariant = Color(0x26818CF8),
    )
}

fun createNoteyShapes(style: UIStyle): Shapes {
    return if (style == UIStyle.ROUNDED) {
        Shapes(
            extraSmall = RoundedCornerShape(6.dp),
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp),
            extraLarge = RoundedCornerShape(24.dp)
        )
    } else {
        // Precision compact industrial scale (4dp/8dp/12dp)
        Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(12.dp),
            extraLarge = RoundedCornerShape(16.dp)
        )
    }
}

val NoteyTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp
    )
)

val MonoMetadataStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.3.sp,
    color = NoteySurfaces.TextMuted
)

@Composable
fun NoteyTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val preset = AccentColors.presets.getOrElse(settings.accentIndex) { AccentColors.presets[0] }

    val colorScheme = when (settings.themeMode) {
        ThemeMode.AMOLED_BLACK -> createAmoledColorScheme(preset)
        ThemeMode.ZINC_GREY -> createZincColorScheme(preset)
        ThemeMode.MONET_DYNAMIC -> createDynamicColorScheme(preset)
        ThemeMode.DYNAMIC_LIQUID_GLASS -> createLiquidGlassColorScheme(preset)
    }

    val shapes = createNoteyShapes(settings.uiStyle)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NoteyTypography,
        shapes = shapes,
        content = content
    )
}

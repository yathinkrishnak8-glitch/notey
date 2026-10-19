package com.pocketdev.notey

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

val NoteAccentColors = listOf(
    Color(0xFFA855F7), // Amethyst
    Color(0xFF06B6D4), // Cyan / Aurora
    Color(0xFF3B82F6), // Blue / Sapphire
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFEF4444), // Ruby
    Color(0xFFEC4899), // Rose
    Color(0xFF64748B)  // Slate Neutral
)

fun getNoteColor(index: Int): Color {
    val i = (index % NoteAccentColors.size + NoteAccentColors.size) % NoteAccentColors.size
    return NoteAccentColors[i]
}

fun createDarkColorScheme(preset: ColorPreset): ColorScheme {
    return darkColorScheme(
        primary = preset.primary,
        onPrimary = Color.White,
        primaryContainer = preset.surfaceGradientStart,
        onPrimaryContainer = preset.tertiary,
        secondary = preset.secondary,
        onSecondary = Color.White,
        secondaryContainer = preset.surfaceGradientEnd,
        onSecondaryContainer = preset.tertiary,
        tertiary = preset.tertiary,
        onTertiary = Color.Black,
        background = Color(0xFF0B0F19),
        onBackground = Color(0xFFF1F5F9),
        surface = Color(0xFF131A2A),
        onSurface = Color(0xFFE2E8F0),
        surfaceVariant = Color(0xFF1E293B),
        onSurfaceVariant = Color(0xFF94A3B8),
        outline = Color(0xFF334155),
        outlineVariant = Color(0xFF1E293B),
    )
}

fun createLightColorScheme(preset: ColorPreset): ColorScheme {
    return lightColorScheme(
        primary = preset.primary,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFF3E8FF),
        onPrimaryContainer = preset.primary,
        secondary = preset.secondary,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE0F2FE),
        onSecondaryContainer = preset.secondary,
        tertiary = preset.tertiary,
        onTertiary = Color.White,
        background = Color(0xFFF8FAFC),
        onBackground = Color(0xFF0F172A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1E293B),
        surfaceVariant = Color(0xFFF1F5F9),
        onSurfaceVariant = Color(0xFF64748B),
        outline = Color(0xFFCBD5E1),
        outlineVariant = Color(0xFFE2E8F0),
    )
}

fun createNoteyShapes(style: UIStyle): Shapes {
    return if (style == UIStyle.ROUNDED) {
        Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(26.dp),
            extraLarge = RoundedCornerShape(32.dp)
        )
    } else {
        Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(20.dp)
        )
    }
}

val NoteyTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.25.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun NoteyTheme(
    settings: AppSettings,
    content: @Composable () -> Unit
) {
    val isDark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val preset = AccentColors.presets.getOrElse(settings.accentIndex) { AccentColors.presets[0] }
    val colorScheme = if (isDark) createDarkColorScheme(preset) else createLightColorScheme(preset)
    val shapes = createNoteyShapes(settings.uiStyle)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NoteyTypography,
        shapes = shapes,
        content = content
    )
}

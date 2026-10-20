package com.pocketdev.notey

import android.content.Context
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val label: String) {
    AMOLED_BLACK("AMOLED Black"),
    ZINC_GREY("Zinc Grey"),
    MONET_DYNAMIC("Monet Dynamic")
}

enum class UIStyle {
    ROUNDED, COMPACT
}

enum class AnimationSpeed(val durationMs: Int, val label: String) {
    INSTANT(0, "Instant (Zero Latency)"),
    FLUID(240, "Fluid (240ms)"),
    SPRING(380, "Smooth Spring (Physics)")
}

data class ColorPreset(
    val name: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val surfaceGradientStart: Color,
    val surfaceGradientEnd: Color,
)

object AccentColors {
    // 6 Dynamic Accent swatches (Luminance clamped 60%-75%)
    val presets = listOf(
        ColorPreset(
            name = "Electric Slate",
            primary = Color(0xFF38BDF8),
            secondary = Color(0xFF0284C7),
            tertiary = Color(0xFFBAE6FD),
            surfaceGradientStart = Color(0xFF0C1929),
            surfaceGradientEnd = Color(0xFF000000)
        ),
        ColorPreset(
            name = "Nordic Mint",
            primary = Color(0xFF34D399),
            secondary = Color(0xFF059669),
            tertiary = Color(0xFFA7F3D0),
            surfaceGradientStart = Color(0xFF062319),
            surfaceGradientEnd = Color(0xFF000000)
        ),
        ColorPreset(
            name = "Solar Amber",
            primary = Color(0xFFFBBF24),
            secondary = Color(0xFFD97706),
            tertiary = Color(0xFFFDE68A),
            surfaceGradientStart = Color(0xFF261805),
            surfaceGradientEnd = Color(0xFF000000)
        ),
        ColorPreset(
            name = "Hyper Violet",
            primary = Color(0xFFA78BFA),
            secondary = Color(0xFF7C3AED),
            tertiary = Color(0xFFDDD6FE),
            surfaceGradientStart = Color(0xFF1E1035),
            surfaceGradientEnd = Color(0xFF000000)
        ),
        ColorPreset(
            name = "Crimson Pulse",
            primary = Color(0xFFFB7185),
            secondary = Color(0xFFE11D48),
            tertiary = Color(0xFFFECDD3),
            surfaceGradientStart = Color(0xFF280B13),
            surfaceGradientEnd = Color(0xFF000000)
        ),
        ColorPreset(
            name = "Monochrome Pure",
            primary = Color(0xFFF4F4F5),
            secondary = Color(0xFF71717A),
            tertiary = Color(0xFFD4D4D8),
            surfaceGradientStart = Color(0xFF18181B),
            surfaceGradientEnd = Color(0xFF000000)
        )
    )

    // Palette of accent dots for quick selection
    val accentSwatches = listOf(
        Color(0xFF38BDF8), // Electric Slate
        Color(0xFF34D399), // Nordic Mint
        Color(0xFFFBBF24), // Solar Amber
        Color(0xFFA78BFA), // Hyper Violet
        Color(0xFFFB7185), // Crimson Pulse
        Color(0xFFF4F4F5)  // Monochrome Pure
    )
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.AMOLED_BLACK,
    val accentIndex: Int = 0,
    val uiStyle: UIStyle = UIStyle.COMPACT,
    val dynamicColor: Boolean = false,
    val animationsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val animationSpeed: AnimationSpeed = AnimationSpeed.SPRING,
    val hapticFeedback: Boolean = true,
    val confirmDestructive: Boolean = true,
    val hasSeenOnboarding: Boolean = false,
    val noteSortBy: String = "updated", // "updated", "created", "title"
    val defaultNoteColor: Int = 0,
    val autoSave: Boolean = true,
    val canvasGrid: Boolean = true,
    val canvasSnapToGrid: Boolean = false,
    val canvasBackground: String = "grid", // "grid", "dots", "blank"
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("notey_settings", Context.MODE_PRIVATE)

    companion object {
        const val GITHUB_REPO_URL = "https://github.com/yathinkrishnak8-glitch/notey"
        const val APP_VERSION = "1.0.0"
        const val BUILD_NUMBER = "1"
        const val DEVELOPER_ATTRIBUTION = "Built by Yathin"
    }

    fun loadSettings(): AppSettings {
        val themeModeStr = prefs.getString("theme_mode", ThemeMode.AMOLED_BLACK.name) ?: ThemeMode.AMOLED_BLACK.name
        val themeMode = try {
            when (themeModeStr) {
                "SYSTEM" -> ThemeMode.MONET_DYNAMIC
                "LIGHT" -> ThemeMode.ZINC_GREY
                "DARK" -> ThemeMode.AMOLED_BLACK
                else -> ThemeMode.valueOf(themeModeStr)
            }
        } catch (_: Exception) {
            ThemeMode.AMOLED_BLACK
        }

        val uiStyleStr = prefs.getString("ui_style", UIStyle.COMPACT.name) ?: UIStyle.COMPACT.name
        val uiStyle = try { UIStyle.valueOf(uiStyleStr) } catch (_: Exception) { UIStyle.COMPACT }

        val animSpeedStr = prefs.getString("anim_speed", AnimationSpeed.SPRING.name) ?: AnimationSpeed.SPRING.name
        val animSpeed = try {
            when (animSpeedStr) {
                "SLOW" -> AnimationSpeed.SPRING
                "NORMAL" -> AnimationSpeed.FLUID
                "FAST" -> AnimationSpeed.INSTANT
                else -> AnimationSpeed.valueOf(animSpeedStr)
            }
        } catch (_: Exception) {
            AnimationSpeed.SPRING
        }

        return AppSettings(
            themeMode = themeMode,
            accentIndex = prefs.getInt("accent_index", 0).coerceIn(0, AccentColors.presets.lastIndex),
            uiStyle = uiStyle,
            dynamicColor = prefs.getBoolean("dynamic_color", false),
            animationsEnabled = prefs.getBoolean("animations_enabled", true),
            reduceMotion = prefs.getBoolean("reduce_motion", false),
            animationSpeed = animSpeed,
            hapticFeedback = prefs.getBoolean("haptic_feedback", true),
            confirmDestructive = prefs.getBoolean("confirm_destructive", true),
            hasSeenOnboarding = prefs.getBoolean("has_seen_onboarding", false),
            noteSortBy = prefs.getString("note_sort_by", "updated") ?: "updated",
            defaultNoteColor = prefs.getInt("default_note_color", 0),
            autoSave = prefs.getBoolean("auto_save", true),
            canvasGrid = prefs.getBoolean("canvas_grid", true),
            canvasSnapToGrid = prefs.getBoolean("canvas_snap_grid", false),
            canvasBackground = prefs.getString("canvas_bg", "grid") ?: "grid"
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString("theme_mode", settings.themeMode.name)
            .putInt("accent_index", settings.accentIndex)
            .putString("ui_style", settings.uiStyle.name)
            .putBoolean("dynamic_color", settings.dynamicColor)
            .putBoolean("animations_enabled", settings.animationsEnabled)
            .putBoolean("reduce_motion", settings.reduceMotion)
            .putString("anim_speed", settings.animationSpeed.name)
            .putBoolean("haptic_feedback", settings.hapticFeedback)
            .putBoolean("confirm_destructive", settings.confirmDestructive)
            .putBoolean("has_seen_onboarding", settings.hasSeenOnboarding)
            .putString("note_sort_by", settings.noteSortBy)
            .putInt("default_note_color", settings.defaultNoteColor)
            .putBoolean("auto_save", settings.autoSave)
            .putBoolean("canvas_grid", settings.canvasGrid)
            .putBoolean("canvas_snap_grid", settings.canvasSnapToGrid)
            .putString("canvas_bg", settings.canvasBackground)
            .apply()
    }
}

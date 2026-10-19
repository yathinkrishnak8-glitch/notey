package com.pocketdev.notey

import android.content.Context
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

enum class UIStyle {
    ROUNDED, COMPACT
}

enum class AnimationSpeed(val durationMs: Int) {
    SLOW(350),
    NORMAL(200),
    FAST(100)
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
    val presets = listOf(
        ColorPreset(
            name = "Amethyst Glow",
            primary = Color(0xFF9333EA),
            secondary = Color(0xFFA855F7),
            tertiary = Color(0xFFC084FC),
            surfaceGradientStart = Color(0xFF1E1035),
            surfaceGradientEnd = Color(0xFF0F071D)
        ),
        ColorPreset(
            name = "Aurora Borealis",
            primary = Color(0xFF06B6D4),
            secondary = Color(0xFF10B981),
            tertiary = Color(0xFF34D399),
            surfaceGradientStart = Color(0xFF072127),
            surfaceGradientEnd = Color(0xFF031014)
        ),
        ColorPreset(
            name = "Sunset Ember",
            primary = Color(0xFFF97316),
            secondary = Color(0xFFEF4444),
            tertiary = Color(0xFFFBBF24),
            surfaceGradientStart = Color(0xFF281308),
            surfaceGradientEnd = Color(0xFF130904)
        ),
        ColorPreset(
            name = "Midnight Obsidian",
            primary = Color(0xFF3B82F6),
            secondary = Color(0xFF6366F1),
            tertiary = Color(0xFF93C5FD),
            surfaceGradientStart = Color(0xFF0B132B),
            surfaceGradientEnd = Color(0xFF060B18)
        ),
        ColorPreset(
            name = "Emerald Grove",
            primary = Color(0xFF10B981),
            secondary = Color(0xFF059669),
            tertiary = Color(0xFF6EE7B7),
            surfaceGradientStart = Color(0xFF062319),
            surfaceGradientEnd = Color(0xFF02100B)
        ),
        ColorPreset(
            name = "Rose Petal",
            primary = Color(0xFFF43F5E),
            secondary = Color(0xFFE11D48),
            tertiary = Color(0xFFFDA4AF),
            surfaceGradientStart = Color(0xFF280B13),
            surfaceGradientEnd = Color(0xFF140509)
        ),
        ColorPreset(
            name = "Frost Light",
            primary = Color(0xFF0EA5E9),
            secondary = Color(0xFF6366F1),
            tertiary = Color(0xFF38BDF8),
            surfaceGradientStart = Color(0xFF1E293B),
            surfaceGradientEnd = Color(0xFF0F172A)
        )
    )

    // Palette of accent dots for quick selection
    val accentSwatches = listOf(
        Color(0xFF9333EA), // Purple
        Color(0xFF06B6D4), // Cyan
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFFEF4444), // Red
        Color(0xFFF43F5E), // Rose
        Color(0xFF64748B)  // Slate
    )
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentIndex: Int = 0,
    val uiStyle: UIStyle = UIStyle.ROUNDED,
    val dynamicColor: Boolean = false,
    val animationsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val animationSpeed: AnimationSpeed = AnimationSpeed.NORMAL,
    val hapticFeedback: Boolean = true,
    val confirmDestructive: Boolean = true,
    val hasSeenOnboarding: Boolean = false,
    val noteSortBy: String = "updated", // "updated", "created", "title"
    val defaultNoteColor: Int = 0,
    val autoSave: Boolean = true,
    val canvasGrid: Boolean = true,
    val canvasSnapToGrid: Boolean = false,
    val canvasBackground: String = "dots", // "dots", "grid", "blank"
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
        val themeModeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val themeMode = try { ThemeMode.valueOf(themeModeStr) } catch (_: Exception) { ThemeMode.SYSTEM }

        val uiStyleStr = prefs.getString("ui_style", UIStyle.ROUNDED.name) ?: UIStyle.ROUNDED.name
        val uiStyle = try { UIStyle.valueOf(uiStyleStr) } catch (_: Exception) { UIStyle.ROUNDED }

        val animSpeedStr = prefs.getString("anim_speed", AnimationSpeed.NORMAL.name) ?: AnimationSpeed.NORMAL.name
        val animSpeed = try { AnimationSpeed.valueOf(animSpeedStr) } catch (_: Exception) { AnimationSpeed.NORMAL }

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
            canvasBackground = prefs.getString("canvas_bg", "dots") ?: "dots"
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

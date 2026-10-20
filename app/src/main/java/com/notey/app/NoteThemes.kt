package com.notey.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Luxury Industrial Color Palette & Accent System (Linear / Obsidian style)
 */
data class GoogleNoteColor(
    val id: Int,
    val name: String,
    val lightColor: Color,
    val darkColor: Color,
    val borderLight: Color,
    val borderDark: Color,
    val accentColor: Color = Color(0xFF38BDF8)
)

object GoogleKeepPalette {
    val colors = listOf(
        GoogleNoteColor(
            id = 0,
            name = "Obsidian",
            lightColor = Color(0xFFFFFFFF),
            darkColor = Color(0xFF09090B),
            borderLight = Color(0xFFE2E8F0),
            borderDark = Color(0x14FFFFFF),
            accentColor = Color(0xFF38BDF8) // Electric Slate
        ),
        GoogleNoteColor(
            id = 1,
            name = "Crimson Pulse",
            lightColor = Color(0xFFFFF1F2),
            darkColor = Color(0xFF13090B),
            borderLight = Color(0xFFFECDD3),
            borderDark = Color(0x14FFFFFF),
            accentColor = Color(0xFFFB7185) // Crimson
        ),
        GoogleNoteColor(
            id = 2,
            name = "Solar Amber",
            lightColor = Color(0xFFFFFBEB),
            darkColor = Color(0xFF140F05),
            borderLight = Color(0xFFFDE68A),
            borderDark = Color(0x14FFFFFF),
            accentColor = Color(0xFFFBBF24) // Solar Amber
        ),
        GoogleNoteColor(
            id = 3,
            name = "Sand Dunes",
            lightColor = Color(0xFFFEFCE8),
            darkColor = Color(0xFF141208),
            borderLight = Color(0xFFFEF08A),
            borderDark = Color(0x14FFFFFF),
            accentColor = Color(0xFFEAB308)
        ),
        GoogleNoteColor(
            id = 4,
            name = "Nordic Mint",
            lightColor = Color(0xFFECFDF5),
            darkColor = Color(0xFF06140D),
            borderLight = Color(0xFFA7F3D0),
            borderDark = Color(0x14FFFFFF),
            accentColor = Color(0xFF34D399) // Nordic Mint
        ),
        GoogleNoteColor(
            id = 5,
            name = "Sage Teal",
            lightColor = Color(0xFFF0FDFA),
            darkColor = Color(0xFF061314),
            borderLight = Color(0xFF99F6E4),
            borderDark = Color(0x14FFFFFF),
            accentColor = Color(0xFF2DD4BF)
        ),
        GoogleNoteColor(
            id = 6,
            name = "Electric Slate",
            lightColor = Color(0xFFF0F9FF),
            darkColor = Color(0xFF08101C),
            borderLight = Color(0xFFBAE6FD),
            borderDark = Color(0x14FFFFFF),
            accentColor = Color(0xFF38BDF8)
        ),
        GoogleNoteColor(
            id = 7,
            name = "Cobalt Storm",
            lightColor = Color(0xFFEEF2FF),
            darkColor = Color(0xFF0A0D1B),
            borderLight = Color(0xFFC7D2FE),
            borderDark = Color(0x14FFFFFF),
            accentColor = Color(0xFF818CF8)
        ),
        GoogleNoteColor(
            id = 8,
            name = "Hyper Violet",
            lightColor = Color(0xFFFAF5FF),
            darkColor = Color(0xFF11091A),
            borderLight = Color(0xFFE9D5FF),
            borderDark = Color(0x14FFFFFF),
            accentColor = Color(0xFFA78BFA) // Hyper Violet
        ),
        GoogleNoteColor(
            id = 9,
            name = "Rose Petal",
            lightColor = Color(0xFFFDF2F8),
            darkColor = Color(0xFF170912),
            borderLight = Color(0xFFFBCFE8),
            borderDark = Color(0x14FFFFFF),
            accentColor = Color(0xFFF472B6)
        ),
        GoogleNoteColor(
            id = 10,
            name = "Warm Stone",
            lightColor = Color(0xFFFAF8F5),
            darkColor = Color(0xFF12100E),
            borderLight = Color(0xFFE7E5E4),
            borderDark = Color(0x14FFFFFF),
            accentColor = Color(0xFFA8A29E)
        ),
        GoogleNoteColor(
            id = 11,
            name = "Monochrome",
            lightColor = Color(0xFFF4F4F5),
            darkColor = Color(0xFF121215),
            borderLight = Color(0xFFE4E4E7),
            borderDark = Color(0x14FFFFFF),
            accentColor = Color(0xFFF4F4F5) // Monochrome Pure
        ),
    )

    fun getColor(id: Int, isDark: Boolean): Color {
        val entry = colors.firstOrNull { it.id == id } ?: colors[0]
        return if (isDark) entry.darkColor else entry.lightColor
    }

    fun getAccentColor(id: Int): Color {
        val entry = colors.firstOrNull { it.id == id } ?: colors[0]
        return entry.accentColor
    }

    fun getBorderColor(id: Int, isDark: Boolean): Color {
        val entry = colors.firstOrNull { it.id == id } ?: colors[0]
        return if (isDark) entry.borderDark else entry.borderLight
    }

    fun getTextColor(id: Int, isDark: Boolean): Color {
        return if (isDark) {
            Color(0xFFFFFFFF)
        } else {
            Color(0xFF09090B)
        }
    }

    fun getSecondaryTextColor(id: Int, isDark: Boolean): Color {
        return if (isDark) {
            Color(0xFFA1A1AA)
        } else {
            Color(0xFF71717A)
        }
    }
}

/**
 * Minimalist background design rendering.
 */
@Composable
fun NoteBackgroundDesign(themeId: String, isDark: Boolean, modifier: Modifier = Modifier) {
    if (themeId == "none" || themeId.isBlank()) return

    val artColor = if (isDark) Color(0x0CFFFFFF) else Color(0x08000000)

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (themeId) {
            "grocery" -> {
                // Precision grid lines
                var x = 0f
                while (x < w) {
                    drawLine(artColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                    x += 24f
                }
            }
            "beach" -> {
                // Subtle horizontal contour
                val path = Path().apply {
                    moveTo(0f, h * 0.75f)
                    cubicTo(w * 0.35f, h * 0.70f, w * 0.65f, h * 0.80f, w, h * 0.72f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path, artColor)
            }
            "notes" -> {
                // Architectural isometric dot pattern
                var x = 12f
                while (x < w) {
                    var y = 12f
                    while (y < h) {
                        drawCircle(artColor, radius = 1.2f, center = Offset(x, y))
                        y += 24f
                    }
                    x += 24f
                }
            }
            else -> {
                // Subtle corner accent radial
                drawCircle(
                    color = artColor,
                    radius = w * 0.45f,
                    center = Offset(w, h)
                )
            }
        }
    }
}

package com.notey.app

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class OnboardingSlide(
    val title: String,
    val subtitle: String,
    val category: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accentColor: Color
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val slides = remember {
        listOf(
            OnboardingSlide(
                title = "100% Offline & Private",
                subtitle = "Zero telemetry, zero external trackers. All notes, attachments, and visual workflows reside solely in your device's local vault.",
                category = "DATA SOVEREIGNTY",
                icon = NoteyIcons.Vault,
                accentColor = Color(0xFF34D399) // Nordic Mint
            ),
            OnboardingSlide(
                title = "Dual-Engine Workspace",
                subtitle = "Seamlessly switch between structured Markdown documentation and an infinite, touch-first visual node-graph canvas.",
                category = "HYBRID PRODUCTIVITY",
                icon = NoteyIcons.Workflow,
                accentColor = Color(0xFF38BDF8) // Electric Slate
            ),
            OnboardingSlide(
                title = "Engineered for AMOLED",
                subtitle = "Constructed with true OLED pure black (#000000), 5-tier industrial surface elevation, and automated WCAG 2.1 contrast inversion.",
                category = "INDUSTRIAL DESIGN",
                icon = NoteyIcons.OledBlack,
                accentColor = Color(0xFFA78BFA) // Hyper Violet
            )
        )
    }

    var currentSlideIndex by remember { mutableIntStateOf(0) }
    val currentSlide = slides[currentSlideIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        // Skip Button at top right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = onFinish,
                colors = ButtonDefaults.textButtonColors(contentColor = NoteySurfaces.TextSecondary)
            ) {
                Text("Skip", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }

        // Slide Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Category Badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = currentSlide.accentColor.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, currentSlide.accentColor.copy(alpha = 0.3f))
            ) {
                Text(
                    text = currentSlide.category,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentSlide.accentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Central Icon Container
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NoteySurfaces.Surface1)
                    .border(1.dp, NoteySurfaces.HairlineBorderStrong, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = currentSlide.icon,
                    contentDescription = null,
                    tint = currentSlide.accentColor,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = currentSlide.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = NoteySurfaces.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Subtitle
            Text(
                text = currentSlide.subtitle,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = NoteySurfaces.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Pagination Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                slides.forEachIndexed { idx, slide ->
                    val isActive = idx == currentSlideIndex
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (isActive) 24.dp else 6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (isActive) slide.accentColor else NoteySurfaces.Surface4)
                            .clickable { currentSlideIndex = idx }
                    )
                }
            }
        }

        // Bottom Navigation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentSlideIndex > 0) {
                IconButton(
                    onClick = { currentSlideIndex-- },
                    modifier = Modifier
                        .size(44.dp)
                        .background(NoteySurfaces.Surface2, CircleShape)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous",
                        tint = NoteySurfaces.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(44.dp))
            }

            if (currentSlideIndex < slides.size - 1) {
                Button(
                    onClick = { currentSlideIndex++ },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = currentSlide.accentColor,
                        contentColor = Color(0xFF000000)
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Next", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                // Final Action: "Initialize Vault"
                Button(
                    onClick = onFinish,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38BDF8),
                        contentColor = Color(0xFF000000)
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Initialize Vault", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

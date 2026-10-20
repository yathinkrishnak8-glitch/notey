package com.notey.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinish: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashAura")
    
    // Ambient breathing pulse for the monogram
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Specular shimmer rotation
    val shimmerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerRotation"
    )

    var progress by remember { mutableFloatStateOf(0f) }
    var phaseText by remember { mutableStateOf("Initializing Workspace...") }

    LaunchedEffect(Unit) {
        val totalSteps = 40
        for (i in 1..totalSteps) {
            delay(35)
            progress = i.toFloat() / totalSteps.toFloat()
            if (progress > 0.7f) {
                phaseText = "Ready"
            } else if (progress > 0.35f) {
                phaseText = "Restoring Canvas Boards..."
            }
        }
        delay(150)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .clickable(onClick = onFinish),
        contentAlignment = Alignment.Center
    ) {
        // Subtle ambient radial glow behind the emblem
        Canvas(modifier = Modifier.size(240.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x3338BDF8),
                        Color(0x15A78BFA),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.minDimension / 1.8f
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // High-End Cybernetic Prism Monogram Shield (84×84dp)
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF111827),
                                Color(0xFF090D16),
                                Color(0xFF030712)
                            )
                        )
                    )
                    .border(
                        1.2.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0x6638BDF8),
                                Color(0x33A78BFA),
                                Color(0x2234D399)
                            )
                        ),
                        RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Vector Geometric "N" Origami Fold
                Canvas(modifier = Modifier.size(84.dp)) {
                    val w = size.width
                    val h = size.height

                    // Left Stem (Electric Cyan)
                    val leftStem = Path().apply {
                        moveTo(w * 0.28f, h * 0.30f)
                        lineTo(w * 0.40f, h * 0.23f)
                        lineTo(w * 0.40f, h * 0.70f)
                        lineTo(w * 0.28f, h * 0.77f)
                        close()
                    }
                    drawPath(
                        path = leftStem,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                        )
                    )

                    // Diagonal Bridge (Hyper Violet)
                    val bridge = Path().apply {
                        moveTo(w * 0.40f, h * 0.23f)
                        lineTo(w * 0.72f, h * 0.70f)
                        lineTo(w * 0.60f, h * 0.77f)
                        lineTo(w * 0.28f, h * 0.30f)
                        close()
                    }
                    drawPath(
                        path = bridge,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFC084FC), Color(0xFF818CF8), Color(0xFF4F46E5))
                        )
                    )

                    // Right Stem (Nordic Mint)
                    val rightStem = Path().apply {
                        moveTo(w * 0.60f, h * 0.30f)
                        lineTo(w * 0.72f, h * 0.23f)
                        lineTo(w * 0.72f, h * 0.70f)
                        lineTo(w * 0.60f, h * 0.77f)
                        close()
                    }
                    drawPath(
                        path = rightStem,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF34D399), Color(0xFF0EA5E9))
                        )
                    )

                    // Specular highlight crest
                    drawCircle(
                        color = Color.White,
                        radius = 2.5f * density,
                        center = Offset(w * 0.72f, h * 0.23f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand Typography
            Text(
                text = "Notey",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEDEDED),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Elegant Subtitle
            Text(
                text = "Local-First Creative Workspace",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.2.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Minimalist Hairline Progress Dock
            Column(
                modifier = Modifier.width(220.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .height(2.5.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF38BDF8),
                                        Color(0xFF818CF8),
                                        Color(0xFF34D399)
                                    )
                                )
                            )
                    )
                }

                Text(
                    text = phaseText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

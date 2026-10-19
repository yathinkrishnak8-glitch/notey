package com.pocketdev.notey

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    val infiniteTransition = rememberInfiniteTransition(label = "SplashPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val steps = 30
        for (i in 1..steps) {
            delay(50)
            progress = i.toFloat() / steps.toFloat()
        }
        delay(200)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .clickable(onClick = onFinish),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Official Notey Logo: 72×72dp squircle container (Surface 1 #09090B, 1px border)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF09090B))
                    .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Vector node graph (circle origin at (22,22) connected via stepped orthogonal line to square terminal at (42,34))
                Canvas(modifier = Modifier.size(72.dp)) {
                    val primaryColor = Color(0xFF38BDF8)
                    val terminalColor = Color(0xFFEDEDED)

                    // Origin circle at (22, 22)
                    drawCircle(
                        color = primaryColor,
                        radius = 4.5f * density,
                        center = Offset(22f * density, 22f * density)
                    )

                    // Stepped orthogonal line: (22, 22) -> (42, 22) -> (42, 34)
                    val path = Path().apply {
                        moveTo(22f * density, 22f * density)
                        lineTo(42f * density, 22f * density)
                        lineTo(42f * density, 34f * density)
                    }
                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = 2f * density)
                    )

                    // Square terminal at (42, 34)
                    val squareSize = 7f * density
                    drawRect(
                        color = terminalColor,
                        topLeft = Offset(42f * density - squareSize / 2, 34f * density - squareSize / 2),
                        size = Size(squareSize, squareSize)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Typographic title "Notey" (24sp Medium #EDEDED)
            Text(
                text = "Notey",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFEDEDED),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle "Version 1.0.0 (Build 1)"
            Text(
                text = "Version 1.0.0 (Build 1)",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color(0xFF71717A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline "Luxury Industrial Local-First Workspace"
            Text(
                text = "Luxury Industrial Local-First Workspace",
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFA1A1AA)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Hairline progress bar with status text
            Column(
                modifier = Modifier.width(200.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFF27272A), RoundedCornerShape(1.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .height(2.dp)
                            .background(Color(0xFF38BDF8), RoundedCornerShape(1.dp))
                    )
                }

                Text(
                    text = "Decrypting local SQLite vault...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = Color(0xFF38BDF8)
                )
            }
        }
    }
}

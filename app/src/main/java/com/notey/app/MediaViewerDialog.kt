package com.notey.app

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import java.io.File
import java.text.DecimalFormat

@Composable
fun MediaViewerDialog(
    filePath: String,
    title: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(filePath) { File(filePath) }
    val isVideo = remember(filePath) {
        val ext = file.extension.lowercase()
        ext in listOf("mp4", "mkv", "webm", "mov", "3gp", "avi")
    }

    val displayTitle = remember(filePath, title) {
        title.ifBlank { file.name.ifBlank { "Media Attachment" } }
    }

    val fileSizeText = remember(filePath) {
        if (file.exists()) {
            val bytes = file.length()
            val df = DecimalFormat("#,##0.#")
            when {
                bytes >= 1024 * 1024 -> "${df.format(bytes / (1024.0 * 1024.0))} MB"
                bytes >= 1024 -> "${df.format(bytes / 1024.0)} KB"
                else -> "$bytes B"
            }
        } else "0 B"
    }

    fun shareMediaFile() {
        if (!file.exists()) return
        FileEngine.shareFile(context, file, if (isVideo) "video/*" else "image/*", displayTitle)
    }

    fun saveMediaToDevice() {
        if (!file.exists()) return
        val mime = if (isVideo) "video/*" else "image/*"
        FileEngine.copyFileToDownloads(context, file, mime).onSuccess { path ->
            Toast.makeText(context, "Saved to $path", Toast.LENGTH_SHORT).show()
        }.onFailure { err ->
            Toast.makeText(context, "Save failed: ${err.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler(enabled = true) {
            onDismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
        ) {
            if (isVideo) {
                VideoPlayerContent(file = file)
            } else {
                ImageViewerContent(file = file)
            }

            // Top Luxury Industrial Overlay Header Bar
            Surface(
                color = Color(0xCC09090B),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(NoteySurfaces.Surface2, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFEDEDED),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            color = Color(0xFFEDEDED),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = NoteySurfaces.Surface3,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = if (isVideo) "VIDEO • $fileSizeText" else "IMAGE • $fileSizeText",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { saveMediaToDevice() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(NoteySurfaces.Surface2, CircleShape)
                    ) {
                        Icon(
                            NoteyIcons.Download,
                            contentDescription = "Save to Device",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { shareMediaFile() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(NoteySurfaces.Surface2, CircleShape)
                    ) {
                        Icon(
                            NoteyIcons.Share,
                            contentDescription = "Share",
                            tint = Color(0xFFEDEDED),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageViewerContent(file: File) {
    val bitmap = remember(file.absolutePath) {
        try {
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        } catch (_: Exception) {
            null
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(file.absolutePath) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = if (scale > 1.2f) 1f else 2.5f
                        offset = Offset.Zero
                    }
                )
            }
            .pointerInput(file.absolutePath) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.8f, 5.0f)
                    scale = newScale
                    offset = if (newScale > 1.0f) offset + pan else Offset.Zero
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = file.name,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Unable to load image",
                    color = NoteySurfaces.TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = file.name,
                    color = NoteySurfaces.TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun VideoPlayerContent(file: File) {
    var isPlaying by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var currentPos by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoViewRef?.let { vv ->
                currentPos = vv.currentPosition
                if (duration == 0 && vv.duration > 0) duration = vv.duration
            }
            delay(500)
        }
    }

    fun formatDuration(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (file.exists()) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        setVideoPath(file.absolutePath)
                        setOnPreparedListener { mp ->
                            mediaPlayerRef = mp
                            duration = mp.duration
                            mp.isLooping = true
                            start()
                            isPlaying = true
                        }
                        setOnCompletionListener {
                            isPlaying = false
                        }
                        videoViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("Video file not found", color = NoteySurfaces.TextMuted)
        }

        // Bottom playback controls dock
        Surface(
            color = Color(0xCC09090B),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Seek slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatDuration(currentPos),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFEDEDED)
                    )

                    Slider(
                        value = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f,
                        onValueChange = { fraction ->
                            val target = (fraction * duration).toInt()
                            currentPos = target
                            videoViewRef?.seekTo(target)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = NoteySurfaces.Surface4
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    Text(
                        text = formatDuration(duration),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NoteySurfaces.TextMuted
                    )
                }

                // Action buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            videoViewRef?.let { vv ->
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(NoteySurfaces.Surface3, CircleShape)
                    ) {
                        Text(
                            text = if (isPlaying) "||" else ">",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFFEDEDED)
                        )
                    }

                    // Mute Toggle Button
                    IconButton(
                        onClick = {
                            isMuted = !isMuted
                            mediaPlayerRef?.let { mp ->
                                val vol = if (isMuted) 0f else 1f
                                mp.setVolume(vol, vol)
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(NoteySurfaces.Surface3, CircleShape)
                    ) {
                        Text(
                            text = if (isMuted) "MUTED" else "AUDIO",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isMuted) Color(0xFFFB7185) else Color(0xFF34D399)
                        )
                    }
                }
            }
        }
    }
}

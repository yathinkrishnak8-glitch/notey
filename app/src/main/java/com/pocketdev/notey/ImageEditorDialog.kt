package com.pocketdev.notey

import android.content.Context
import android.graphics.*
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@Composable
fun ImageEditorDialog(
    imagePath: String,
    onDismiss: () -> Unit,
    onSaveSuccess: (savedPath: String) -> Unit
) {
    val context = LocalContext.current
    val originalFile = remember { File(imagePath) }

    val baseBitmap = remember(imagePath) {
        if (originalFile.exists()) {
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
                BitmapFactory.decodeFile(imagePath, opts)
            } catch (_: Exception) { null }
        } else null
    }

    if (baseBitmap == null) {
        Toast.makeText(context, "Could not open image for editing", Toast.LENGTH_SHORT).show()
        onDismiss()
        return
    }

    var currentBitmap by remember { mutableStateOf(baseBitmap) }
    val undoStack = remember { mutableStateListOf<Bitmap>() }
    val redoStack = remember { mutableStateListOf<Bitmap>() }

    fun pushHistory(newBmp: Bitmap) {
        undoStack.add(currentBitmap)
        redoStack.clear()
        currentBitmap = newBmp
    }

    fun rotate(degrees: Float) {
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(currentBitmap, 0, 0, currentBitmap.width, currentBitmap.height, matrix, true)
        pushHistory(rotated)
    }

    fun flip(horizontal: Boolean) {
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f) else postScale(1f, -1f)
        }
        val flipped = Bitmap.createBitmap(currentBitmap, 0, 0, currentBitmap.width, currentBitmap.height, matrix, true)
        pushHistory(flipped)
    }

    fun applyCrop(ratioX: Int, ratioY: Int) {
        val w = currentBitmap.width
        val h = currentBitmap.height
        val targetRatio = ratioX.toFloat() / ratioY.toFloat()
        val currentRatio = w.toFloat() / h.toFloat()

        var cropW = w
        var cropH = h
        if (currentRatio > targetRatio) {
            cropW = (h * targetRatio).toInt()
        } else {
            cropH = (w / targetRatio).toInt()
        }
        val startX = ((w - cropW) / 2).coerceAtLeast(0)
        val startY = ((h - cropH) / 2).coerceAtLeast(0)

        val cropped = Bitmap.createBitmap(currentBitmap, startX, startY, cropW, cropH)
        pushHistory(cropped)
    }

    fun applyFilter(type: String) {
        val output = Bitmap.createBitmap(currentBitmap.width, currentBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cm = when (type) {
            "GRAYSCALE" -> ColorMatrix().apply { setSaturation(0f) }
            "INVERT" -> ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            "SEPIA" -> ColorMatrix().apply {
                setSaturation(0f)
                val sepia = ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 30f,
                        0f, 1f, 0f, 0f, 15f,
                        0f, 0f, 0.8f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                postConcat(sepia)
            }
            else -> ColorMatrix()
        }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(currentBitmap, 0f, 0f, paint)
        pushHistory(output)
    }

    fun saveImage(asCopy: Boolean) {
        try {
            val targetFile = if (asCopy) {
                val dir = File(context.filesDir, "attachments").apply { mkdirs() }
                File(dir, "edited_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png")
            } else {
                originalFile
            }

            FileOutputStream(targetFile).use { out ->
                currentBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            Toast.makeText(context, if (asCopy) "Saved as new copy" else "Image updated", Toast.LENGTH_SHORT).show()
            onSaveSuccess(targetFile.absolutePath)
            onDismiss()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Save error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Text(
                        text = "Image Editor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(
                            onClick = {
                                if (undoStack.isNotEmpty()) {
                                    val prev = undoStack.removeAt(undoStack.lastIndex)
                                    redoStack.add(currentBitmap)
                                    currentBitmap = prev
                                }
                            },
                            enabled = undoStack.isNotEmpty()
                        ) {
                            Text("↺", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = {
                                if (redoStack.isNotEmpty()) {
                                    val next = redoStack.removeAt(redoStack.lastIndex)
                                    undoStack.add(currentBitmap)
                                    currentBitmap = next
                                }
                            },
                            enabled = redoStack.isNotEmpty()
                        ) {
                            Text("↻", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Image Preview Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = currentBitmap.asImageBitmap(),
                        contentDescription = "Editing Canvas",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                // Editor Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Transform Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(onClick = { rotate(90f) }) {
                            Text("↷ 90°")
                        }
                        FilledTonalButton(onClick = { rotate(-90f) }) {
                            Text("↶ -90°")
                        }
                        FilledTonalButton(onClick = { flip(true) }) {
                            Text("⇄ Flip H")
                        }
                        FilledTonalButton(onClick = { flip(false) }) {
                            Text("⇅ Flip V")
                        }
                        FilledTonalButton(onClick = { applyCrop(1, 1) }) {
                            Text("1:1 Crop")
                        }
                        FilledTonalButton(onClick = { applyCrop(16, 9) }) {
                            Text("16:9 Crop")
                        }
                        FilledTonalButton(onClick = { applyFilter("GRAYSCALE") }) {
                            Text("B&W")
                        }
                        FilledTonalButton(onClick = { applyFilter("SEPIA") }) {
                            Text("Sepia")
                        }
                        FilledTonalButton(onClick = { applyFilter("INVERT") }) {
                            Text("Invert")
                        }
                    }

                    // Save Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { saveImage(asCopy = true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save as Copy")
                        }
                        Button(
                            onClick = { saveImage(asCopy = false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Overwrite Save")
                        }
                    }
                }
            }
        }
    }
}

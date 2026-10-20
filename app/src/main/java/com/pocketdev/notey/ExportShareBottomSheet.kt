package com.pocketdev.notey

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class ExportFormat(val title: String, val extension: String, val mimeType: String, val desc: String) {
    MARKDOWN("Markdown", ".md", "text/markdown", "Raw structured text with headings & tasks"),
    PLAIN_TEXT("Plain Text", ".txt", "text/plain", "Clean stripped text with zero markup"),
    PDF_DOCUMENT("Publication Document", ".pdf", "application/pdf", "Styled printable PDF layout"),
    WEB_PAGE("Standalone Web Page", ".html", "text/html", "Self-contained HTML with AMOLED styling"),
    WORKFLOW_DIAGRAM("Workflow Diagram", ".png", "image/png", "High-res raster canvas / preview banner"),
    NODE_GRAPH("Node Graph", ".json", "application/json", "Machine-readable node & link schema")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportShareBottomSheet(
    note: Note,
    onDismiss: () -> Unit,
    onSavedSuccessfully: (() -> Unit)? = null
) {
    ExportShareBottomSheet(
        title = note.title,
        content = note.body,
        canvasDoc = null,
        onDismiss = onDismiss,
        onSavedSuccessfully = onSavedSuccessfully
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportShareBottomSheet(
    title: String,
    content: String,
    canvasDoc: CanvasDocument? = null,
    onDismiss: () -> Unit,
    onSavedSuccessfully: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportFormat.MARKDOWN) }
    var isGenerating by remember { mutableStateOf(false) }

    val cleanTitle = remember(title) {
        title.trim().ifBlank { "Untitled_Export" }.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    fun generateExportFile(format: ExportFormat): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val targetFile = File(exportDir, "$cleanTitle${format.extension}")

        when (format) {
            ExportFormat.MARKDOWN -> {
                val header = "# $title\n*Exported from Notey on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}*\n\n"
                targetFile.writeText(header + content)
            }
            ExportFormat.PLAIN_TEXT -> {
                val stripped = content
                    .replace(Regex("#+\\s*"), "")
                    .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                    .replace(Regex("\\*(.*?)\\*"), "$1")
                    .replace("- [x] ", "[x] ")
                    .replace("- [ ] ", "[ ] ")
                targetFile.writeText("$title\n\n$stripped")
            }
            ExportFormat.PDF_DOCUMENT -> {
                val doc = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas

                // Background
                val bgPaint = Paint().apply { color = android.graphics.Color.rgb(18, 18, 20) }
                canvas.drawRect(0f, 0f, 595f, 842f, bgPaint)

                // Header accent bar
                val accentPaint = Paint().apply { color = android.graphics.Color.rgb(56, 189, 248) }
                canvas.drawRect(40f, 40f, 555f, 44f, accentPaint)

                // Title
                val titlePaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 22f
                    isFakeBoldText = true
                }
                canvas.drawText(title.ifBlank { "Untitled" }, 40f, 75f, titlePaint)

                // Monospace ribbon
                val metaPaint = Paint().apply {
                    color = android.graphics.Color.rgb(161, 161, 170)
                    textSize = 10f
                }
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                canvas.drawText("NOTEY EXPORT • $dateStr • LOCAL VAULT", 40f, 95f, metaPaint)

                // Body text (word wrapped)
                val bodyPaint = Paint().apply {
                    color = android.graphics.Color.rgb(228, 228, 231)
                    textSize = 12f
                }
                var currentY = 125f
                val lines = content.lines()
                for (rawLine in lines) {
                    if (currentY > 800f) break
                    val words = rawLine.split(" ")
                    var curLine = ""
                    for (word in words) {
                        val testLine = if (curLine.isEmpty()) word else "$curLine $word"
                        if (bodyPaint.measureText(testLine) > 515f) {
                            canvas.drawText(curLine, 40f, currentY, bodyPaint)
                            currentY += 18f
                            curLine = word
                            if (currentY > 800f) break
                        } else {
                            curLine = testLine
                        }
                    }
                    if (curLine.isNotEmpty() && currentY <= 800f) {
                        canvas.drawText(curLine, 40f, currentY, bodyPaint)
                        currentY += 18f
                    }
                }

                doc.finishPage(page)
                FileOutputStream(targetFile).use { fos -> doc.writeTo(fos) }
                doc.close()
            }
            ExportFormat.WEB_PAGE -> {
                val escapedContent = content.replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br/>\n")
                val html = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <title>${title.ifBlank { "Notey Export" }}</title>
                        <style>
                            body {
                                background-color: #09090B;
                                color: #EDEDED;
                                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace;
                                margin: 0;
                                padding: 24px;
                                line-height: 1.6;
                            }
                            .container {
                                max-width: 760px;
                                margin: 0 auto;
                                background-color: #121215;
                                border: 1px solid rgba(255,255,255,0.08);
                                border-radius: 12px;
                                padding: 32px;
                            }
                            h1 { color: #38BDF8; margin-top: 0; font-size: 26px; }
                            .meta { font-family: monospace; font-size: 11px; color: #71717A; margin-bottom: 24px; border-bottom: 1px solid rgba(255,255,255,0.08); padding-bottom: 12px; }
                            .content { font-size: 15px; color: #D4D4D8; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <h1>${title.ifBlank { "Untitled Note" }}</h1>
                            <div class="meta">NOTEY LOCAL-FIRST WORKSPACE • EXPORTED DOCUMENT</div>
                            <div class="content">$escapedContent</div>
                        </div>
                    </body>
                    </html>
                """.trimIndent()
                targetFile.writeText(html)
            }
            ExportFormat.WORKFLOW_DIAGRAM -> {
                // High-resolution raster banner / canvas diagram
                val bmp = Bitmap.createBitmap(1200, 800, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(android.graphics.Color.rgb(9, 9, 11))

                val linePaint = Paint().apply {
                    color = android.graphics.Color.rgb(39, 39, 42)
                    strokeWidth = 2f
                }
                // Dot grid
                for (x in 0 until 1200 step 40) {
                    for (y in 0 until 800 step 40) {
                        canvas.drawCircle(x.toFloat(), y.toFloat(), 2f, linePaint)
                    }
                }

                val cardPaint = Paint().apply {
                    color = android.graphics.Color.rgb(24, 24, 27)
                    style = Paint.Style.FILL
                }
                val borderPaint = Paint().apply {
                    color = android.graphics.Color.rgb(56, 189, 248)
                    strokeWidth = 3f
                    style = Paint.Style.STROKE
                }
                val textPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 28f
                    isFakeBoldText = true
                }
                val subTextPaint = Paint().apply {
                    color = android.graphics.Color.rgb(161, 161, 170)
                    textSize = 18f
                }

                if (canvasDoc != null && canvasDoc.nodes.isNotEmpty()) {
                    // Draw actual canvas nodes
                    for (node in canvasDoc.nodes) {
                        val nx = (node.x + 300f).coerceIn(40f, 1000f)
                        val ny = (node.y + 200f).coerceIn(40f, 650f)
                        canvas.drawRoundRect(nx, ny, nx + 200f, ny + 100f, 16f, 16f, cardPaint)
                        canvas.drawRoundRect(nx, ny, nx + 200f, ny + 100f, 16f, 16f, borderPaint)
                        canvas.drawText(node.title.take(14), nx + 16f, ny + 40f, textPaint)
                        canvas.drawText(node.type.name, nx + 16f, ny + 75f, subTextPaint)
                    }
                } else {
                    // Draw formatted note summary block
                    canvas.drawRoundRect(100f, 100f, 1100f, 700f, 24f, 24f, cardPaint)
                    canvas.drawRoundRect(100f, 100f, 1100f, 700f, 24f, 24f, borderPaint)
                    canvas.drawText(title.ifBlank { "Notey Workspace Document" }, 140f, 180f, textPaint)
                    canvas.drawText("Format: Workflow Diagram Raster • High-Resolution Export", 140f, 230f, subTextPaint)
                }

                FileOutputStream(targetFile).use { fos ->
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
            }
            ExportFormat.NODE_GRAPH -> {
                val json = JSONObject().apply {
                    put("app", "Notey")
                    put("version", "1.0.0")
                    put("exportedAt", System.currentTimeMillis())
                    put("title", title)
                    if (canvasDoc != null) {
                        put("type", "canvas")
                        put("documentId", canvasDoc.id)
                        val nodesArr = JSONArray()
                        canvasDoc.nodes.forEach { n ->
                            nodesArr.put(JSONObject().apply {
                                put("id", n.id)
                                put("title", n.title)
                                put("type", n.type.name)
                                put("x", n.x)
                                put("y", n.y)
                                put("width", n.width)
                                put("height", n.height)
                                put("content", n.content)
                            })
                        }
                        put("nodes", nodesArr)
                        val connArr = JSONArray()
                        canvasDoc.connectors.forEach { c ->
                            connArr.put(JSONObject().apply {
                                put("id", c.id)
                                put("from", c.fromNodeId)
                                put("to", c.toNodeId)
                            })
                        }
                        put("connectors", connArr)
                    } else {
                        put("type", "note")
                        put("content", content)
                    }
                }
                targetFile.writeText(json.toString(2))
            }
        }
        return targetFile
    }

    fun triggerSaveToDevice(format: ExportFormat) {
        try {
            isGenerating = true
            val file = generateExportFile(format)
            val result = FileEngine.copyFileToDownloads(context, file, format.mimeType)
            result.onSuccess { path ->
                Toast.makeText(context, "Saved to $path", Toast.LENGTH_LONG).show()
                onSavedSuccessfully?.invoke()
                onDismiss()
            }.onFailure { err ->
                Toast.makeText(context, "Save failed: ${err.message}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Save error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isGenerating = false
        }
    }

    fun triggerNativeShare(format: ExportFormat) {
        try {
            isGenerating = true
            val file = generateExportFile(format)
            val shared = FileEngine.shareFile(context, file, format.mimeType, title)
            if (shared) {
                onSavedSuccessfully?.invoke()
                onDismiss()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            isGenerating = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NoteySurfaces.Surface1,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(NoteySurfaces.Surface4, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Export & Share Studio",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NoteySurfaces.TextPrimary
                    )
                    Text(
                        text = "Select multi-format output target",
                        style = MonoMetadataStyle,
                        color = NoteySurfaces.TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NoteySurfaces.TextSecondary)
                }
            }

            // Output format selection cards
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExportFormat.values().forEach { format ->
                    val isSelected = selectedFormat == format
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) NoteySurfaces.Surface2 else NoteySurfaces.Surface1,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF38BDF8) else NoteySurfaces.HairlineBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = format.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color(0xFF38BDF8) else NoteySurfaces.TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = NoteySurfaces.Surface3
                                    ) {
                                        Text(
                                            text = format.extension,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color(0xFF38BDF8) else NoteySurfaces.TextMuted,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = format.desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NoteySurfaces.TextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { triggerSaveToDevice(selectedFormat) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NoteySurfaces.TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorderStrong),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(NoteyIcons.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Device", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { triggerNativeShare(selectedFormat) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38BDF8),
                        contentColor = Color(0xFF000000)
                    ),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(NoteyIcons.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share File", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

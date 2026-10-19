package com.pocketdev.notey

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoStudioScreen(
    initialProject: LogoProject? = null,
    onBack: () -> Unit,
    onSaved: (LogoProject) -> Unit = {}
) {
    val context = LocalContext.current
    val logoStore = remember { LogoProjectStore(context) }
    val assetStore = remember { AssetLibraryStore(context) }

    var currentProject by remember {
        mutableStateOf(initialProject ?: LogoProject())
    }

    var showAssetPicker by remember { mutableStateOf(false) }

    val activeAsset = remember(currentProject.symbolAssetId) {
        assetStore.allAssets.firstOrNull { it.id == currentProject.symbolAssetId }
            ?: assetStore.allAssets.first()
    }

    val symbolColor = remember(currentProject.symbolColorHex) {
        try { Color(android.graphics.Color.parseColor(currentProject.symbolColorHex)) } catch (_: Exception) { Color(0xFF6366F1) }
    }
    val textColor = remember(currentProject.textColorHex) {
        try { Color(android.graphics.Color.parseColor(currentProject.textColorHex)) } catch (_: Exception) { Color(0xFF0F172A) }
    }
    val taglineColor = remember(currentProject.taglineColorHex) {
        try { Color(android.graphics.Color.parseColor(currentProject.taglineColorHex)) } catch (_: Exception) { Color(0xFF64748B) }
    }
    val bgColor = remember(currentProject.bgColorHex) {
        try { Color(android.graphics.Color.parseColor(currentProject.bgColorHex)) } catch (_: Exception) { Color(0xFFF8FAFC) }
    }

    fun exportLogoPng(proj: LogoProject) {
        try {
            val size = 512
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor(proj.bgColorHex)
            }

            // Draw background container
            when (proj.bgShape) {
                "ROUNDED" -> canvas.drawRoundRect(RectF(16f, 16f, size - 16f, size - 16f), 48f, 48f, bgPaint)
                "CIRCLE" -> canvas.drawCircle(size / 2f, size / 2f, size / 2f - 16f, bgPaint)
                "NONE" -> {} // Transparent
                else -> canvas.drawRoundRect(RectF(16f, 16f, size - 16f, size - 16f), 24f, 24f, bgPaint)
            }

            // Draw brand text
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor(proj.textColorHex)
                textSize = proj.fontSize * 2.2f
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val taglinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor(proj.taglineColorHex)
                textSize = (proj.fontSize * 1.1f).coerceAtLeast(18f)
                textAlign = Paint.Align.CENTER
            }

            if (proj.layoutMode == "HORIZONTAL") {
                canvas.drawText(proj.brandText, size * 0.6f, size * 0.48f, textPaint)
                if (proj.taglineText.isNotBlank()) {
                    canvas.drawText(proj.taglineText, size * 0.6f, size * 0.58f, taglinePaint)
                }
            } else {
                canvas.drawText(proj.brandText, size / 2f, size * 0.68f, textPaint)
                if (proj.taglineText.isNotBlank()) {
                    canvas.drawText(proj.taglineText, size / 2f, size * 0.78f, taglinePaint)
                }
            }

            val dir = File(context.filesDir, "logos").apply { mkdirs() }
            val file = File(dir, "logo_${proj.id}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Logo"))
            Toast.makeText(context, "Logo exported successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentProject.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { exportLogoPng(currentProject) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export PNG")
                    }
                    Button(
                        onClick = {
                            logoStore.save(currentProject)
                            onSaved(currentProject)
                            Toast.makeText(context, "Logo project saved", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Live Interactive Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Container Shape
                val containerShape = when (currentProject.bgShape) {
                    "CIRCLE" -> CircleShape
                    "ROUNDED" -> RoundedCornerShape(28.dp)
                    "NONE" -> RoundedCornerShape(0.dp)
                    else -> RoundedCornerShape(16.dp)
                }

                Surface(
                    shape = containerShape,
                    color = if (currentProject.bgShape == "NONE") Color.Transparent else bgColor,
                    shadowElevation = if (currentProject.bgShape == "NONE") 0.dp else 4.dp,
                    border = if (currentProject.bgShape == "NONE") null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(230.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentProject.layoutMode == "HORIZONTAL") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AssetVectorRenderer(
                                    asset = activeAsset,
                                    modifier = Modifier.size(currentProject.symbolSize.coerceAtMost(64).dp),
                                    tint = symbolColor
                                )
                                Spacer(modifier = Modifier.width(currentProject.spacing.dp))
                                Column {
                                    Text(
                                        text = currentProject.brandText.ifBlank { "Brand" },
                                        fontSize = currentProject.fontSize.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        maxLines = 1
                                    )
                                    if (currentProject.taglineText.isNotBlank()) {
                                        Text(
                                            text = currentProject.taglineText,
                                            fontSize = (currentProject.fontSize * 0.55f).sp,
                                            color = taglineColor,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        } else if (currentProject.layoutMode == "SYMBOL_ONLY") {
                            AssetVectorRenderer(
                                asset = activeAsset,
                                modifier = Modifier.size(currentProject.symbolSize.dp),
                                tint = symbolColor
                            )
                        } else {
                            // VERTICAL (Default)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                AssetVectorRenderer(
                                    asset = activeAsset,
                                    modifier = Modifier.size(currentProject.symbolSize.dp),
                                    tint = symbolColor
                                )
                                Spacer(modifier = Modifier.height(currentProject.spacing.dp))
                                Text(
                                    text = currentProject.brandText.ifBlank { "Brand" },
                                    fontSize = currentProject.fontSize.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                if (currentProject.taglineText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = currentProject.taglineText,
                                        fontSize = (currentProject.fontSize * 0.52f).sp,
                                        color = taglineColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Controls & Customization
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Symbol Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AssetVectorRenderer(
                                    asset = activeAsset,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(activeAsset.name, fontWeight = FontWeight.SemiBold)
                            Text(activeAsset.category.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    OutlinedButton(onClick = { showAssetPicker = true }) {
                        Text("Change Symbol")
                    }
                }

                HorizontalDivider()

                // Brand Name & Tagline TextFields
                OutlinedTextField(
                    value = currentProject.brandText,
                    onValueChange = { currentProject = currentProject.copy(brandText = it) },
                    label = { Text("Brand Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = currentProject.taglineText,
                    onValueChange = { currentProject = currentProject.copy(taglineText = it) },
                    label = { Text("Tagline / Subtitle (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Layout Mode Selector
                Text("Layout Mode:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "VERTICAL" to "Stacked",
                        "HORIZONTAL" to "Side-by-Side",
                        "SYMBOL_ONLY" to "Mark Only"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = currentProject.layoutMode == mode,
                            onClick = { currentProject = currentProject.copy(layoutMode = mode) },
                            label = { Text(label) }
                        )
                    }
                }

                // Background Container Shape
                Text("Background Container:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ROUNDED" to "Squircle",
                        "CIRCLE" to "Circle",
                        "NONE" to "Transparent"
                    ).forEach { (shape, label) ->
                        FilterChip(
                            selected = currentProject.bgShape == shape,
                            onClick = { currentProject = currentProject.copy(bgShape = shape) },
                            label = { Text(label) }
                        )
                    }
                }

                // Color Palette Tints
                Text("Symbol Color Tint:", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf(
                        "#6366F1", "#06B6D4", "#10B981", "#F59E0B",
                        "#EF4444", "#EC4899", "#8B5CF6", "#0F172A"
                    ).forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { currentProject = currentProject.copy(symbolColorHex = hex) }
                                .border(
                                    width = if (currentProject.symbolColorHex == hex) 3.dp else 1.dp,
                                    color = if (currentProject.symbolColorHex == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Background Color
                if (currentProject.bgShape != "NONE") {
                    Text("Container Background Color:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            "#F8FAFC", "#FFFFFF", "#F1F5F9", "#FEF3C7",
                            "#ECFDF5", "#EFF6FF", "#FAF5FF", "#0F172A"
                        ).forEach { hex ->
                            val col = Color(android.graphics.Color.parseColor(hex))
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .clickable { currentProject = currentProject.copy(bgColorHex = hex) }
                                    .border(
                                        width = if (currentProject.bgColorHex == hex) 3.dp else 1.dp,
                                        color = if (currentProject.bgColorHex == hex) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                // Size Sliders
                Text("Symbol Scale (${currentProject.symbolSize} dp):", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = currentProject.symbolSize.toFloat(),
                    onValueChange = { currentProject = currentProject.copy(symbolSize = it.toInt()) },
                    valueRange = 40f..120f
                )

                Text("Text Size (${currentProject.fontSize} sp):", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = currentProject.fontSize.toFloat(),
                    onValueChange = { currentProject = currentProject.copy(fontSize = it.toInt()) },
                    valueRange = 16f..36f
                )
            }
        }
    }

    if (showAssetPicker) {
        AssetPickerSheet(
            onDismiss = { showAssetPicker = false },
            onAssetSelected = { asset ->
                currentProject = currentProject.copy(symbolAssetId = asset.id)
                showAssetPicker = false
            },
            title = "Choose Logo Symbol"
        )
    }
}

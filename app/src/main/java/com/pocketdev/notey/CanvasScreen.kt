package com.pocketdev.notey

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    canvasStore: CanvasStore,
    settings: AppSettings,
    noteStore: NoteStore? = null,
    initialDocId: String? = null,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var documents by remember { mutableStateOf(canvasStore.loadAll()) }
    var selectedDocId by remember {
        mutableStateOf(initialDocId ?: documents.firstOrNull()?.id ?: "")
    }

    var currentDoc by remember(selectedDocId) {
        val doc = documents.firstOrNull { it.id == selectedDocId }
            ?: documents.firstOrNull()
            ?: CanvasDocument(name = "Workspace", type = CanvasType.CANVAS).also {
                canvasStore.save(it)
                documents = canvasStore.loadAll()
            }
        mutableStateOf(doc)
    }

    // Undo / Redo Stacks
    val undoStack = remember(currentDoc.id) { mutableStateListOf<CanvasDocument>() }
    val redoStack = remember(currentDoc.id) { mutableStateListOf<CanvasDocument>() }

    fun pushUndo() {
        undoStack.add(currentDoc)
        if (undoStack.size > 25) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
    }

    fun applyUndo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(currentDoc)
            currentDoc = previous
            canvasStore.save(previous)
            documents = canvasStore.loadAll()
        }
    }

    fun applyRedo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(currentDoc)
            currentDoc = next
            canvasStore.save(next)
            documents = canvasStore.loadAll()
        }
    }

    fun commitDocChange(updated: CanvasDocument, recordUndo: Boolean = true) {
        if (recordUndo) {
            pushUndo()
        }
        currentDoc = updated
        canvasStore.save(updated)
        documents = canvasStore.loadAll()
    }

    var scale by remember(currentDoc.id) { mutableStateOf(currentDoc.zoom.coerceIn(0.4f, 3.0f)) }
    var panOffset by remember(currentDoc.id) { mutableStateOf(Offset(currentDoc.panX, currentDoc.panY)) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    val selNode = currentDoc.nodes.firstOrNull { it.id == selectedNodeId }
    var showAddNodeDialog by remember { mutableStateOf(false) }
    var showEditNodeDialog by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showDocSelector by remember { mutableStateOf(false) }
    var connectSourceNodeId by remember { mutableStateOf<String?>(null) }
    var activeMediaPreview by remember { mutableStateOf<CanvasNode?>(null) }

    // File picker launcher for canvas media cards
    var pendingMediaNodeType by remember { mutableStateOf(CanvasNodeType.IMAGE) }
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && noteStore != null) {
            val att = noteStore.copyUriToInternalStorage(uri)
            if (att != null) {
                val isVid = att.type == AttachmentType.VIDEO
                val newNode = CanvasNode(
                    id = UUID.randomUUID().toString(),
                    type = if (isVid) CanvasNodeType.VIDEO else CanvasNodeType.IMAGE,
                    title = att.displayName,
                    content = if (isVid) "Video attachment" else "Image attachment",
                    x = 100f - panOffset.x / scale,
                    y = 120f - panOffset.y / scale,
                    width = 200f,
                    height = 160f,
                    mediaPath = att.filePath,
                    mediaType = if (isVid) "video" else "image",
                    colorIndex = 1
                )
                commitDocChange(currentDoc.copy(nodes = currentDoc.nodes + newNode))
                selectedNodeId = newNode.id
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Main Bar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }

                // Workspace title & dropdown trigger
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDocSelector = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currentDoc.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "▾",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (currentDoc.type == CanvasType.WORKFLOW) "Visual Workflow" else "Freeform Canvas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Undo / Redo controls
                IconButton(
                    onClick = { applyUndo() },
                    enabled = undoStack.isNotEmpty()
                ) {
                    Text(
                        text = "↺",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                IconButton(
                    onClick = { applyRedo() },
                    enabled = redoStack.isNotEmpty()
                ) {
                    Text(
                        text = "↻",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }

                // Zoom controls pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "–",
                            modifier = Modifier
                                .clickable { scale = (scale - 0.2f).coerceAtLeast(0.4f) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(scale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
                        Text(
                            text = "+",
                            modifier = Modifier
                                .clickable { scale = (scale + 0.2f).coerceAtMost(3.0f) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Add Object Button
                FilledTonalIconButton(
                    onClick = { showAddNodeDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Object")
                }
            }
        }

        // Contextual Selection Toolbar
        if (selNode != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = selNode.title.ifBlank { selNode.type.name },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 110.dp)
                    )

                    // Edit / Rename button
                    FilledTonalButton(
                        onClick = { showEditNodeDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("✏️ Edit", fontSize = 12.sp)
                    }

                    // Connect button
                    FilledTonalButton(
                        onClick = {
                            connectSourceNodeId = selNode.id
                            showConnectDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("🔗 Connect", fontSize = 12.sp)
                    }

                    // Duplicate button
                    FilledTonalButton(
                        onClick = {
                            val cloned = selNode.copy(
                                id = UUID.randomUUID().toString(),
                                x = selNode.x + 24f,
                                y = selNode.y + 24f,
                                title = if (selNode.title.isNotBlank()) "${selNode.title} (Copy)" else ""
                            )
                            commitDocChange(currentDoc.copy(nodes = currentDoc.nodes + cloned))
                            selectedNodeId = cloned.id
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("🔄 Copy", fontSize = 12.sp)
                    }

                    // Bring Forward (reorder to end of list)
                    IconButton(
                        onClick = {
                            val reordered = currentDoc.nodes.filter { it.id != selNode.id } + selNode
                            commitDocChange(currentDoc.copy(nodes = reordered))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("▲", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    // Send Backward (reorder to start of list)
                    IconButton(
                        onClick = {
                            val reordered = listOf(selNode) + currentDoc.nodes.filter { it.id != selNode.id }
                            commitDocChange(currentDoc.copy(nodes = reordered))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("▼", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    // Delete button
                    IconButton(
                        onClick = {
                            val remainingNodes = currentDoc.nodes.filter { it.id != selNode.id }
                            val remainingConns = currentDoc.connectors.filter {
                                it.fromNodeId != selNode.id && it.toNodeId != selNode.id
                            }
                            selectedNodeId = null
                            commitDocChange(currentDoc.copy(nodes = remainingNodes, connectors = remainingConns))
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Node", tint = MaterialTheme.colorScheme.error)
                    }

                    // Close selection
                    IconButton(
                        onClick = { selectedNodeId = null },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Deselect")
                    }
                }
            }
        }

        // Interactive 2D Freeform Canvas Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        panOffset += dragAmount
                    }
                }
        ) {
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            val connectorDefaultColor = MaterialTheme.colorScheme.primary

            // Background Canvas: Grid/Dots and Connectors/Arrows
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw background alignment grid/dots
                if (settings.canvasGrid) {
                    val step = 40f * scale
                    val startX = (panOffset.x % step + step) % step
                    val startY = (panOffset.y % step + step) % step

                    var x = startX
                    while (x < canvasWidth) {
                        var y = startY
                        while (y < canvasHeight) {
                            drawCircle(
                                color = gridColor,
                                radius = 2f * scale.coerceIn(0.8f, 2.2f),
                                center = Offset(x, y)
                            )
                            y += step
                        }
                        x += step
                    }
                }

                // Draw Connectors and Arrowheads
                currentDoc.connectors.forEach { connector ->
                    val fromNode = currentDoc.nodes.firstOrNull { it.id == connector.fromNodeId }
                    val toNode = currentDoc.nodes.firstOrNull { it.id == connector.toNodeId }

                    if (fromNode != null && toNode != null) {
                        val fromCenter = Offset(
                            x = panOffset.x + (fromNode.x + fromNode.width / 2f) * scale,
                            y = panOffset.y + (fromNode.y + fromNode.height / 2f) * scale
                        )
                        val toCenter = Offset(
                            x = panOffset.x + (toNode.x + toNode.width / 2f) * scale,
                            y = panOffset.y + (toNode.y + toNode.height / 2f) * scale
                        )

                        val connColor = if (connector.colorIndex > 0) {
                            getNoteColor(connector.colorIndex)
                        } else {
                            connectorDefaultColor
                        }

                        // Draw connector path
                        drawLine(
                            color = connColor,
                            start = fromCenter,
                            end = toCenter,
                            strokeWidth = 3f * scale
                        )

                        // Draw Arrowhead at toCenter
                        val angle = atan2(toCenter.y - fromCenter.y, toCenter.x - fromCenter.x)
                        val arrowLength = 16f * scale
                        val arrowAngle = Math.PI / 6

                        val p1 = Offset(
                            x = toCenter.x - arrowLength * cos(angle - arrowAngle).toFloat(),
                            y = toCenter.y - arrowLength * sin(angle - arrowAngle).toFloat()
                        )
                        val p2 = Offset(
                            x = toCenter.x - arrowLength * cos(angle + arrowAngle).toFloat(),
                            y = toCenter.y - arrowLength * sin(angle + arrowAngle).toFloat()
                        )

                        val path = Path().apply {
                            moveTo(toCenter.x, toCenter.y)
                            lineTo(p1.x, p1.y)
                            lineTo(p2.x, p2.y)
                            close()
                        }
                        drawPath(path, connColor)
                    }
                }
            }

            // Draggable & Resizable Composable Nodes
            currentDoc.nodes.forEach { node ->
                val isSelected = node.id == selectedNodeId
                val nodeColor = getNoteColor(node.colorIndex)

                val shape = when (node.shapeType) {
                    "pill" -> RoundedCornerShape(percent = 50)
                    "circle" -> CircleShape
                    "diamond" -> RoundedCornerShape(12.dp)
                    "rect" -> RoundedCornerShape(4.dp)
                    else -> RoundedCornerShape(14.dp)
                }

                val nodeX = panOffset.x + node.x * scale
                val nodeY = panOffset.y + node.y * scale

                Box(
                    modifier = Modifier
                        .offset { IntOffset(nodeX.roundToInt(), nodeY.roundToInt()) }
                        .size(
                            width = (node.width * scale).dp.coerceAtLeast(80.dp),
                            height = (node.height * scale).dp.coerceAtLeast(48.dp)
                        )
                        .shadow(
                            elevation = if (isSelected) 8.dp else 2.dp,
                            shape = shape
                        )
                        .clip(shape)
                        .background(
                            when (node.type) {
                                CanvasNodeType.START -> Color(0xFF10B981)
                                CanvasNodeType.END -> Color(0xFFEF4444)
                                CanvasNodeType.DECISION -> Color(0xFF0284C7)
                                CanvasNodeType.STICKY -> Color(0xFFFEF08A)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .border(
                            width = if (isSelected) 3.dp else 1.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else nodeColor,
                            shape = shape
                        )
                        // Drag gesture for moving the node
                        .pointerInput(node.id, scale) {
                            var dragStarted = false
                            detectDragGestures(
                                onDragStart = {
                                    dragStarted = true
                                    selectedNodeId = node.id
                                    pushUndo()
                                },
                                onDragEnd = {
                                    dragStarted = false
                                    canvasStore.save(currentDoc)
                                },
                                onDragCancel = {
                                    dragStarted = false
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dx = dragAmount.x / scale
                                    val dy = dragAmount.y / scale
                                    val updatedNodes = currentDoc.nodes.map {
                                        if (it.id == node.id) {
                                            it.copy(x = it.x + dx, y = it.y + dy)
                                        } else it
                                    }
                                    currentDoc = currentDoc.copy(nodes = updatedNodes)
                                }
                            )
                        }
                        .clickable {
                            selectedNodeId = if (isSelected) null else node.id
                        }
                        .padding(8.dp)
                ) {
                    // Content inside the card
                    if (node.type == CanvasNodeType.IMAGE && node.mediaPath.isNotBlank()) {
                        val file = File(node.mediaPath)
                        val bitmap = remember(node.mediaPath) {
                            if (file.exists()) {
                                try { BitmapFactory.decodeFile(node.mediaPath) } catch (_: Exception) { null }
                            } else null
                        }

                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = node.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { activeMediaPreview = node }
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("🖼️", fontSize = 24.sp)
                                Text(node.title, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    } else if (node.type == CanvasNodeType.VIDEO && node.mediaPath.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { activeMediaPreview = node },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🎬", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = node.title.ifBlank { "Video" },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("▶ Tap to preview", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        // Standard text/shape/workflow card content
                        val isDarkCard = node.type in listOf(CanvasNodeType.START, CanvasNodeType.END, CanvasNodeType.DECISION)
                        val textColor = if (isDarkCard) Color.White
                        else if (node.type == CanvasNodeType.STICKY) Color(0xFF1E293B)
                        else MaterialTheme.colorScheme.onSurface

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = node.title.ifBlank { node.type.name },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (node.content.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = node.content,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDarkCard) Color.White.copy(alpha = 0.85f) else textColor.copy(alpha = 0.75f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Resize handle (bottom right corner when selected)
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .pointerInput(node.id, scale) {
                                    detectDragGestures(
                                        onDragStart = { pushUndo() },
                                        onDragEnd = { canvasStore.save(currentDoc) },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val dw = dragAmount.x / scale
                                            val dh = dragAmount.y / scale
                                            val updatedNodes = currentDoc.nodes.map {
                                                if (it.id == node.id) {
                                                    it.copy(
                                                        width = (it.width + dw).coerceAtLeast(80f),
                                                        height = (it.height + dh).coerceAtLeast(48f)
                                                    )
                                                } else it
                                            }
                                            currentDoc = currentDoc.copy(nodes = updatedNodes)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("↘", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Center / Reset View Floating Button
            FilledTonalIconButton(
                onClick = {
                    scale = 1.0f
                    panOffset = Offset(20f, 20f)
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text("⊙", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Edit Node Dialog (Title, Content, Color, Shape)
    if (showEditNodeDialog && selNode != null) {
        val targetNode = selNode
        var editTitle by remember(targetNode.id) { mutableStateOf(targetNode.title) }
        var editContent by remember(targetNode.id) { mutableStateOf(targetNode.content) }
        var editColorIdx by remember(targetNode.id) { mutableStateOf(targetNode.colorIndex) }

        AlertDialog(
            onDismissRequest = { showEditNodeDialog = false },
            title = { Text("Edit Object") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("Subtitle / Content") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Accent Tint:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NoteAccentColors.take(6).forEachIndexed { idx, color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { editColorIdx = idx }
                                    .border(
                                        width = if (editColorIdx == idx) 3.dp else 1.dp,
                                        color = if (editColorIdx == idx) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedNodes = currentDoc.nodes.map {
                            if (it.id == targetNode.id) {
                                it.copy(title = editTitle.trim(), content = editContent.trim(), colorIndex = editColorIdx)
                            } else it
                        }
                        commitDocChange(currentDoc.copy(nodes = updatedNodes))
                        showEditNodeDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Object Dialog
    if (showAddNodeDialog) {
        var nodeTitle by remember { mutableStateOf("") }
        var nodeContent by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(CanvasNodeType.TASK) }
        var selectedColorIdx by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = { showAddNodeDialog = false },
            title = { Text("Add to Workspace") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nodeTitle,
                        onValueChange = { nodeTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nodeContent,
                        onValueChange = { nodeContent = it },
                        label = { Text("Subtitle / Content") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Object Type:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            CanvasNodeType.TASK,
                            CanvasNodeType.DECISION,
                            CanvasNodeType.START,
                            CanvasNodeType.END,
                            CanvasNodeType.STICKY,
                            CanvasNodeType.SHAPE
                        ).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    // Attach photo / video card option
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                pendingMediaNodeType = CanvasNodeType.IMAGE
                                showAddNodeDialog = false
                                mediaPickerLauncher.launch(arrayOf("image/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📷 Add Photo")
                        }

                        OutlinedButton(
                            onClick = {
                                pendingMediaNodeType = CanvasNodeType.VIDEO
                                showAddNodeDialog = false
                                mediaPickerLauncher.launch(arrayOf("video/*"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🎬 Add Video")
                        }
                    }

                    Text("Accent Tint:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NoteAccentColors.take(6).forEachIndexed { idx, color ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorIdx = idx }
                                    .border(
                                        width = if (selectedColorIdx == idx) 3.dp else 1.dp,
                                        color = if (selectedColorIdx == idx) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shape = when (selectedType) {
                            CanvasNodeType.START, CanvasNodeType.END -> "pill"
                            CanvasNodeType.DECISION -> "diamond"
                            CanvasNodeType.STICKY -> "rect"
                            else -> "rounded"
                        }
                        val newNode = CanvasNode(
                            id = UUID.randomUUID().toString(),
                            type = selectedType,
                            title = nodeTitle.ifBlank { selectedType.name },
                            content = nodeContent,
                            x = 100f - panOffset.x / scale,
                            y = 120f - panOffset.y / scale,
                            width = if (selectedType == CanvasNodeType.DECISION) 150f else 140f,
                            height = 65f,
                            colorIndex = selectedColorIdx,
                            shapeType = shape
                        )
                        commitDocChange(currentDoc.copy(nodes = currentDoc.nodes + newNode))
                        selectedNodeId = newNode.id
                        showAddNodeDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Connect Node Dialog
    if (showConnectDialog && connectSourceNodeId != null) {
        val srcNode = currentDoc.nodes.firstOrNull { it.id == connectSourceNodeId }
        val targetNodes = currentDoc.nodes.filter { it.id != connectSourceNodeId }

        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = { Text("Connect to...") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "From: ${srcNode?.title?.ifBlank { srcNode?.type?.name }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Select target node to draw arrow to:", style = MaterialTheme.typography.bodySmall)

                    targetNodes.forEach { target ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newConn = CanvasConnector(
                                        fromNodeId = connectSourceNodeId!!,
                                        toNodeId = target.id,
                                        label = if (srcNode?.type == CanvasNodeType.DECISION) "Next" else ""
                                    )
                                    commitDocChange(currentDoc.copy(connectors = currentDoc.connectors + newConn))
                                    showConnectDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = target.title.ifBlank { target.type.name },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showConnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Media Lightbox / Video Viewer Popup
    if (activeMediaPreview != null) {
        val mNode = activeMediaPreview!!
        val file = File(mNode.mediaPath)
        Dialog(onDismissRequest = { activeMediaPreview = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mNode.title.ifBlank { "Media Preview" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { activeMediaPreview = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (!file.exists()) {
                        Text("File is no longer available on disk", color = MaterialTheme.colorScheme.error)
                    } else if (mNode.type == CanvasNodeType.IMAGE || mNode.mediaType == "image") {
                        val bitmap = remember(mNode.mediaPath) {
                            try { BitmapFactory.decodeFile(mNode.mediaPath) } catch (_: Exception) { null }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = mNode.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 350.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    } else if (mNode.type == CanvasNodeType.VIDEO || mNode.mediaType == "video") {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoPath(mNode.mediaPath)
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    start()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        file
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        val mime = if (mNode.type == CanvasNodeType.VIDEO) "video/*" else "image/*"
                                        setDataAndType(uri, mime)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open media"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open media", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open in App")
                        }

                        // Remove from canvas action
                        OutlinedButton(
                            onClick = {
                                val remainingNodes = currentDoc.nodes.filter { it.id != mNode.id }
                                val remainingConns = currentDoc.connectors.filter {
                                    it.fromNodeId != mNode.id && it.toNodeId != mNode.id
                                }
                                commitDocChange(currentDoc.copy(nodes = remainingNodes, connectors = remainingConns))
                                activeMediaPreview = null
                                selectedNodeId = null
                            }
                        ) {
                            Text("Remove Card")
                        }
                    }
                }
            }
        }
    }

    // Workspaces / Canvas Document Selector
    if (showDocSelector) {
        var newDocTitle by remember { mutableStateOf("") }
        var isWorkflow by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showDocSelector = false },
            title = { Text("Workspaces & Canvases") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    documents.forEach { doc ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (doc.id == currentDoc.id) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedDocId = doc.id
                                    showDocSelector = false
                                }
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (doc.type == CanvasType.WORKFLOW) "Flowchart Process (${doc.nodes.size} nodes)" else "Visual Canvas (${doc.nodes.size} nodes)",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                if (doc.id == currentDoc.id) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Create New Workspace", style = MaterialTheme.typography.titleSmall)
                    OutlinedTextField(
                        value = newDocTitle,
                        onValueChange = { newDocTitle = it },
                        label = { Text("Workspace Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isWorkflow, onCheckedChange = { isWorkflow = it })
                        Text("Flowchart / Workflow mode", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newDocTitle.isNotBlank()) {
                            val newDoc = CanvasDocument(
                                name = newDocTitle.trim(),
                                type = if (isWorkflow) CanvasType.WORKFLOW else CanvasType.CANVAS
                            )
                            canvasStore.save(newDoc)
                            documents = canvasStore.loadAll()
                            selectedDocId = newDoc.id
                            showDocSelector = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDocSelector = false }) {
                    Text("Close")
                }
            }
        )
    }
}

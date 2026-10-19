package com.pocketdev.notey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    canvasStore: CanvasStore,
    settings: AppSettings,
    initialDocId: String? = null,
    onBack: (() -> Unit)? = null
) {
    var documents by remember { mutableStateOf(canvasStore.loadAll()) }
    var selectedDocId by remember {
        mutableStateOf(initialDocId ?: documents.firstOrNull()?.id ?: "")
    }

    val currentDoc = documents.firstOrNull { it.id == selectedDocId }
        ?: documents.firstOrNull()
        ?: CanvasDocument(name = "New Workspace", type = CanvasType.CANVAS).also {
            canvasStore.save(it)
            documents = canvasStore.loadAll()
        }

    var scale by remember(currentDoc.id) { mutableStateOf(currentDoc.zoom.coerceIn(0.5f, 2.5f)) }
    var panOffset by remember(currentDoc.id) { mutableStateOf(Offset(currentDoc.panX, currentDoc.panY)) }
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    var showAddNodeDialog by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var showDocSelector by remember { mutableStateOf(false) }
    var connectSourceNodeId by remember { mutableStateOf<String?>(null) }

    fun updateDoc(updated: CanvasDocument) {
        canvasStore.save(updated)
        documents = canvasStore.loadAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Toolbar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDocSelector = true }
                        .padding(horizontal = 8.dp)
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

                // Zoom controls pill
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "-",
                            modifier = Modifier
                                .clickable { scale = (scale - 0.2f).coerceAtLeast(0.5f) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(scale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "+",
                            modifier = Modifier
                                .clickable { scale = (scale + 0.2f).coerceAtMost(2.5f) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Add Node button
                FilledTonalIconButton(
                    onClick = { showAddNodeDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Node")
                }
            }
        }

        // Sub-bar for selected node actions
        if (selectedNodeId != null) {
            val selNode = currentDoc.nodes.firstOrNull { it.id == selectedNodeId }
            if (selNode != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selected: ${selNode.title.ifBlank { selNode.type.name }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )

                        TextButton(
                            onClick = {
                                connectSourceNodeId = selNode.id
                                showConnectDialog = true
                            }
                        ) {
                            Text("Connect ➜")
                        }

                        IconButton(
                            onClick = {
                                val remainingNodes = currentDoc.nodes.filter { it.id != selNode.id }
                                val remainingConns = currentDoc.connectors.filter {
                                    it.fromNodeId != selNode.id && it.toNodeId != selNode.id
                                }
                                selectedNodeId = null
                                updateDoc(currentDoc.copy(nodes = remainingNodes, connectors = remainingConns))
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Node", tint = MaterialTheme.colorScheme.error)
                        }

                        IconButton(onClick = { selectedNodeId = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Deselect")
                        }
                    }
                }
            }
        }

        // Interactive 2D Infinite Canvas Area
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
            // Background Canvas: Grid/Dots and Connectors/Arrows
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            val connectorDefaultColor = MaterialTheme.colorScheme.primary

            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw background dots or grid
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
                                radius = 2f * scale.coerceIn(0.8f, 2f),
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

                        // Draw line
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

            // Draggable Composable Nodes
            currentDoc.nodes.forEach { node ->
                val nodeX = panOffset.x + node.x * scale
                val nodeY = panOffset.y + node.y * scale
                val isSelected = node.id == selectedNodeId
                val nodeColor = getNoteColor(node.colorIndex)

                val shape = when (node.shapeType) {
                    "pill" -> RoundedCornerShape(percent = 50)
                    "circle" -> CircleShape
                    "diamond" -> RoundedCornerShape(12.dp)
                    else -> RoundedCornerShape(14.dp)
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(nodeX.roundToInt(), nodeY.roundToInt()) }
                        .size(
                            width = (node.width * scale).dp.coerceAtLeast(80.dp),
                            height = (node.height * scale).dp.coerceAtLeast(48.dp)
                        )
                        .shadow(
                            elevation = if (isSelected) 8.dp else 3.dp,
                            shape = shape
                        )
                        .clip(shape)
                        .background(
                            if (node.type == CanvasNodeType.START) Color(0xFF10B981)
                            else if (node.type == CanvasNodeType.END) Color(0xFFEF4444)
                            else if (node.type == CanvasNodeType.DECISION) Color(0xFF0284C7)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            width = if (isSelected) 3.dp else 1.5.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else nodeColor,
                            shape = shape
                        )
                        .pointerInput(node.id, scale) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val updatedNodes = currentDoc.nodes.map {
                                    if (it.id == node.id) {
                                        it.copy(
                                            x = it.x + dragAmount.x / scale,
                                            y = it.y + dragAmount.y / scale
                                        )
                                    } else it
                                }
                                updateDoc(currentDoc.copy(nodes = updatedNodes))
                            }
                        }
                        .clickable {
                            selectedNodeId = if (isSelected) null else node.id
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = node.title.ifBlank { node.type.name },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (node.type in listOf(CanvasNodeType.START, CanvasNodeType.END, CanvasNodeType.DECISION)) Color.White
                            else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (node.content.isNotBlank()) {
                            Text(
                                text = node.content,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (node.type in listOf(CanvasNodeType.START, CanvasNodeType.END, CanvasNodeType.DECISION)) Color.White.copy(alpha = 0.85f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Quick Center / Reset Zoom Floating Button
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

    // Add Node Dialog
    if (showAddNodeDialog) {
        var nodeTitle by remember { mutableStateOf("") }
        var nodeContent by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(CanvasNodeType.TASK) }
        var selectedColorIdx by remember { mutableStateOf(0) }

        AlertDialog(
            onDismissRequest = { showAddNodeDialog = false },
            title = { Text("Add Object") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Object Type:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(CanvasNodeType.TASK, CanvasNodeType.DECISION, CanvasNodeType.STICKY, CanvasNodeType.START).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    Text("Accent Tint:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NoteAccentColors.take(5).forEachIndexed { idx, color ->
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
                            CanvasNodeType.STICKY -> "rounded"
                            else -> "rounded"
                        }
                        val newNode = CanvasNode(
                            id = UUID.randomUUID().toString(),
                            type = selectedType,
                            title = nodeTitle.ifBlank { selectedType.name },
                            content = nodeContent,
                            x = 100f - panOffset.x / scale,
                            y = 150f - panOffset.y / scale,
                            width = if (selectedType == CanvasNodeType.DECISION) 150f else 140f,
                            height = 65f,
                            colorIndex = selectedColorIdx,
                            shapeType = shape
                        )
                        updateDoc(currentDoc.copy(nodes = currentDoc.nodes + newNode))
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
                                    updateDoc(currentDoc.copy(connectors = currentDoc.connectors + newConn))
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

    // Document Selector Modal / Dialog
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
                    Divider()
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

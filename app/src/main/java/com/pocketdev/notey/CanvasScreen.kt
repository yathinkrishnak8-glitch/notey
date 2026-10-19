package com.pocketdev.notey

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasScreen(
    canvasStore: CanvasStore,
    settings: AppSettings,
    noteStore: NoteStore? = null,
    initialDocId: String? = null,
    onBack: (() -> Unit)? = null,
    onOpenNote: ((Long) -> Unit)? = null,
    onOpenFolder: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var documents by remember { mutableStateOf(canvasStore.loadAll()) }
    var activeCanvasId by remember { mutableStateOf<String?>(initialDocId) }

    // Intercept back key to return to canvas overview if inside a canvas
    BackHandler(enabled = activeCanvasId != null) {
        activeCanvasId = null
    }

    if (activeCanvasId == null) {
        CanvasManagerScreen(
            documents = documents,
            onSelectCanvas = { docId -> activeCanvasId = docId },
            onCreateCanvas = { name, type ->
                val newDoc = CanvasDocument(
                    id = UUID.randomUUID().toString(),
                    name = name.ifBlank { "Untitled ${type.name.lowercase().replaceFirstChar { it.uppercase() }}" },
                    type = type,
                    updatedAt = System.currentTimeMillis()
                )
                canvasStore.save(newDoc)
                documents = canvasStore.loadAll()
                activeCanvasId = newDoc.id
            },
            onDuplicateCanvas = { docId ->
                val copy = canvasStore.duplicate(docId)
                if (copy != null) {
                    documents = canvasStore.loadAll()
                    Toast.makeText(context, "Duplicated canvas", Toast.LENGTH_SHORT).show()
                }
            },
            onRenameCanvas = { docId, newName ->
                canvasStore.rename(docId, newName)
                documents = canvasStore.loadAll()
            },
            onDeleteCanvas = { docId ->
                canvasStore.delete(docId)
                documents = canvasStore.loadAll()
                Toast.makeText(context, "Deleted canvas", Toast.LENGTH_SHORT).show()
            },
            onBack = onBack
        )
    } else {
        val currentDoc = documents.firstOrNull { it.id == activeCanvasId }
        if (currentDoc == null) {
            activeCanvasId = null
        } else {
            CanvasWorkspaceScreen(
                initialDoc = currentDoc,
                canvasStore = canvasStore,
                settings = settings,
                noteStore = noteStore,
                onBack = { activeCanvasId = null },
                onOpenNote = onOpenNote,
                onOpenFolder = onOpenFolder
            )
        }
    }
}

// =========================================================================
// CANVAS MANAGER (OVERVIEW SCREEN)
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasManagerScreen(
    documents: List<CanvasDocument>,
    onSelectCanvas: (String) -> Unit,
    onCreateCanvas: (String, CanvasType) -> Unit,
    onDuplicateCanvas: (String) -> Unit,
    onRenameCanvas: (String, String) -> Unit,
    onDeleteCanvas: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<CanvasType?>(null) }
    var sortBy by remember { mutableStateOf("updated") } // "updated", "name", "objects"
    var isGridView by remember { mutableStateOf(false) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var canvasToRename by remember { mutableStateOf<CanvasDocument?>(null) }
    var canvasToDelete by remember { mutableStateOf<CanvasDocument?>(null) }

    val filteredList = remember(documents, searchQuery, selectedTypeFilter, sortBy) {
        var list = documents.filter { doc ->
            val matchType = selectedTypeFilter == null || doc.type == selectedTypeFilter
            val matchQuery = searchQuery.isBlank() || doc.name.contains(searchQuery, ignoreCase = true)
            matchType && matchQuery
        }

        list = when (sortBy) {
            "name" -> list.sortedBy { it.name.lowercase() }
            "objects" -> list.sortedByDescending { it.nodes.size }
            else -> list.sortedByDescending { it.updatedAt }
        }
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Canvases & Workflows", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${documents.size} total workspaces",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            if (isGridView) Icons.Default.List else Icons.Default.Menu,
                            contentDescription = "Toggle Grid/List View"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Canvas")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar & Filter Controls
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search canvases by name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Type Filter Chips & Sort
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("All (${documents.size})") }
                    )
                    FilterChip(
                        selected = selectedTypeFilter == CanvasType.CANVAS,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == CanvasType.CANVAS) null else CanvasType.CANVAS },
                        label = { Text("Freeform") }
                    )
                    FilterChip(
                        selected = selectedTypeFilter == CanvasType.WORKFLOW,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == CanvasType.WORKFLOW) null else CanvasType.WORKFLOW },
                        label = { Text("Workflow") }
                    )
                    FilterChip(
                        selected = selectedTypeFilter == CanvasType.MIND_MAP,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == CanvasType.MIND_MAP) null else CanvasType.MIND_MAP },
                        label = { Text("Mind Map") }
                    )
                    FilterChip(
                        selected = selectedTypeFilter == CanvasType.PLANNING,
                        onClick = { selectedTypeFilter = if (selectedTypeFilter == CanvasType.PLANNING) null else CanvasType.PLANNING },
                        label = { Text("Planning") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Main List / Grid
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No canvases found" else "No canvases yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create a freeform board, visual workflow, or mind map to start organizing visually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Canvas")
                        }
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { doc ->
                        CanvasCard(
                            doc = doc,
                            onClick = { onSelectCanvas(doc.id) },
                            onRename = { canvasToRename = doc },
                            onDuplicate = { onDuplicateCanvas(doc.id) },
                            onDelete = { canvasToDelete = doc }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { doc ->
                        CanvasRow(
                            doc = doc,
                            onClick = { onSelectCanvas(doc.id) },
                            onRename = { canvasToRename = doc },
                            onDuplicate = { onDuplicateCanvas(doc.id) },
                            onDelete = { canvasToDelete = doc }
                        )
                    }
                }
            }
        }
    }

    // Create Canvas Dialog
    if (showCreateDialog) {
        var canvasName by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(CanvasType.CANVAS) }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Canvas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = canvasName,
                        onValueChange = { canvasName = it },
                        label = { Text("Canvas Name") },
                        placeholder = { Text("e.g. Architecture Map") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Mode & Format:", style = MaterialTheme.typography.labelMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            CanvasType.CANVAS to "Freeform Canvas (Notes, Media, Sticky cards)",
                            CanvasType.WORKFLOW to "Visual Workflow (Logic gates & connectors)",
                            CanvasType.MIND_MAP to "Mind Map (Concept branches & idea hierarchy)",
                            CanvasType.PLANNING to "Planning Board (Task planning & roadmaps)"
                        ).forEach { (type, description) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedType = type }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(type.name.lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold)
                                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateCanvas(canvasName, selectedType)
                        showCreateDialog = false
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Canvas Dialog
    if (canvasToRename != null) {
        val target = canvasToRename!!
        var renameText by remember { mutableStateOf(target.name) }

        AlertDialog(
            onDismissRequest = { canvasToRename = null },
            title = { Text("Rename Canvas") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("New Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            onRenameCanvas(target.id, renameText.trim())
                        }
                        canvasToRename = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { canvasToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (canvasToDelete != null) {
        val target = canvasToDelete!!
        AlertDialog(
            onDismissRequest = { canvasToDelete = null },
            title = { Text("Delete Canvas?") },
            text = {
                Text("Are you sure you want to delete \"${target.name}\"? This removes the canvas layout and its placed cards. Underlying notes and library files will NOT be deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteCanvas(target.id)
                        canvasToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { canvasToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CanvasRow(
    doc: CanvasDocument,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val sdf = remember { SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = when (doc.type) {
                    CanvasType.WORKFLOW -> MaterialTheme.colorScheme.tertiaryContainer
                    CanvasType.MIND_MAP -> MaterialTheme.colorScheme.secondaryContainer
                    CanvasType.PLANNING -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        when (doc.type) {
                            CanvasType.WORKFLOW -> Icons.Default.Share
                            CanvasType.MIND_MAP -> Icons.Default.Search
                            CanvasType.PLANNING -> Icons.Default.Check
                            else -> Icons.Default.Edit
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = doc.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(" • ", color = MaterialTheme.colorScheme.outline)
                    Text(
                        text = "${doc.nodes.size} items, ${doc.connectors.size} links",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "Modified ${sdf.format(Date(doc.updatedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Canvas actions")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = { menuExpanded = false; onRename() },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = { menuExpanded = false; onDuplicate() },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CanvasCard(
    doc: CanvasDocument,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { menuExpanded = false; onRename() }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            onClick = { menuExpanded = false; onDuplicate() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; onDelete() }
                        )
                    }
                }
            }

            Column {
                Text(
                    text = doc.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${doc.nodes.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =========================================================================
// TOUCH-FIRST INFINITE CANVAS WORKSPACE SCREEN
// =========================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasWorkspaceScreen(
    initialDoc: CanvasDocument,
    canvasStore: CanvasStore,
    settings: AppSettings,
    noteStore: NoteStore? = null,
    onBack: () -> Unit,
    onOpenNote: ((Long) -> Unit)? = null,
    onOpenFolder: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var currentDoc by remember { mutableStateOf(initialDoc) }

    // Multi-step Undo & Redo History (up to 25 states)
    val undoStack = remember(currentDoc.id) { mutableStateListOf<CanvasDocument>() }
    val redoStack = remember(currentDoc.id) { mutableStateListOf<CanvasDocument>() }

    fun pushUndo() {
        undoStack.add(currentDoc)
        if (undoStack.size > 25) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun applyUndo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(currentDoc)
            currentDoc = prev
            canvasStore.save(prev)
        }
    }

    fun applyRedo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(currentDoc)
            currentDoc = next
            canvasStore.save(next)
        }
    }

    fun commitDocChange(updated: CanvasDocument, recordUndo: Boolean = true) {
        if (recordUndo) pushUndo()
        currentDoc = updated
        canvasStore.save(updated)
    }

    // Viewport & Zoom State
    var scale by remember(currentDoc.id) { mutableStateOf(currentDoc.zoom.coerceIn(0.25f, 4.0f)) }
    var panOffset by remember(currentDoc.id) { mutableStateOf(Offset(currentDoc.panX, currentDoc.panY)) }

    // Active Selection State
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    val selectedNode = currentDoc.nodes.firstOrNull { it.id == selectedNodeId }

    // Dialog & Sheet Controls
    var showAddContentSheet by remember { mutableStateOf(false) }
    var showEditNodeDialog by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var connectSourceNodeId by remember { mutableStateOf<String?>(null) }
    var activeMediaPreview by remember { mutableStateOf<CanvasNode?>(null) }
    var imageToEditPath by remember { mutableStateOf<String?>(null) }
    var showAssetPicker by remember { mutableStateOf(false) }

    // Grid Preferences (Local toggle overrides)
    var isGridVisible by remember { mutableStateOf(settings.canvasGrid) }
    var isSnapToGrid by remember { mutableStateOf(settings.canvasSnapToGrid) }

    // Media Picker Launcher
    var pendingMediaType by remember { mutableStateOf("image") }
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
                    content = if (isVid) "Video Card" else "Photo Card",
                    x = (200f - panOffset.x) / scale,
                    y = (200f - panOffset.y) / scale,
                    width = 200f,
                    height = 160f,
                    mediaPath = att.filePath,
                    mediaType = if (isVid) "video" else "image"
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
        // Top Toolbar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Return to Overview")
                }

                Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                    Text(
                        text = currentDoc.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentDoc.type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Undo & Redo
                IconButton(onClick = { applyUndo() }, enabled = undoStack.isNotEmpty()) {
                    Text("↺", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = { applyRedo() }, enabled = redoStack.isNotEmpty()) {
                    Text("↻", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (redoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline)
                }

                // Zoom Pill
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
                                .clickable { scale = (scale - 0.2f).coerceAtLeast(0.25f) }
                                .padding(horizontal = 6.dp),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${(scale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "+",
                            modifier = Modifier
                                .clickable { scale = (scale + 0.2f).coerceAtMost(4.0f) }
                                .padding(horizontal = 6.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Toggle Grid
                IconButton(onClick = { isGridVisible = !isGridVisible }) {
                    Icon(
                        if (isGridVisible) Icons.Default.Check else Icons.Default.Close,
                        contentDescription = "Toggle Grid",
                        tint = if (isGridVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }

                // Add Object FAB / Button
                FilledTonalIconButton(onClick = { showAddContentSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add to Canvas")
                }
            }
        }

        // Contextual Selection Toolbar
        if (selectedNode != null) {
            val node = selectedNode
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = node.title.ifBlank { node.type.name },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )

                    FilledTonalButton(
                        onClick = { showEditNodeDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Edit", fontSize = 12.sp)
                    }

                    // Open Real Content (if Note or Folder)
                    if (node.noteId != null && onOpenNote != null) {
                        Button(
                            onClick = { onOpenNote(node.noteId) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Open Note", fontSize = 12.sp)
                        }
                    }

                    if (!node.folderName.isNullOrBlank() && onOpenFolder != null) {
                        Button(
                            onClick = { onOpenFolder(node.folderName) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Open Folder", fontSize = 12.sp)
                        }
                    }

                    if (node.type == CanvasNodeType.IMAGE && node.mediaPath.isNotBlank()) {
                        FilledTonalButton(
                            onClick = { imageToEditPath = node.mediaPath },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Edit Image", fontSize = 12.sp)
                        }
                    }

                    // Connect button
                    FilledTonalButton(
                        onClick = {
                            connectSourceNodeId = node.id
                            showConnectDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Connect", fontSize = 12.sp)
                    }

                    // Duplicate button
                    FilledTonalButton(
                        onClick = {
                            val cloned = node.copy(
                                id = UUID.randomUUID().toString(),
                                x = node.x + 24f,
                                y = node.y + 24f,
                                title = if (node.title.isNotBlank()) "${node.title} (Copy)" else ""
                            )
                            commitDocChange(currentDoc.copy(nodes = currentDoc.nodes + cloned))
                            selectedNodeId = cloned.id
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Copy", fontSize = 12.sp)
                    }

                    // Bring Forward
                    IconButton(
                        onClick = {
                            val reordered = currentDoc.nodes.filter { it.id != node.id } + node
                            commitDocChange(currentDoc.copy(nodes = reordered))
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Text("▲", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    // Send Backward
                    IconButton(
                        onClick = {
                            val reordered = listOf(node) + currentDoc.nodes.filter { it.id != node.id }
                            commitDocChange(currentDoc.copy(nodes = reordered))
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Text("▼", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    // Remove from Canvas (Leaves underlying Note/Folder intact)
                    IconButton(
                        onClick = {
                            val remainingNodes = currentDoc.nodes.filter { it.id != node.id }
                            val remainingConns = currentDoc.connectors.filter {
                                it.fromNodeId != node.id && it.toNodeId != node.id
                            }
                            selectedNodeId = null
                            commitDocChange(currentDoc.copy(nodes = remainingNodes, connectors = remainingConns))
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                    }

                    IconButton(
                        onClick = { selectedNodeId = null },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Deselect")
                    }
                }
            }
        }

        // Touch-First Infinite Canvas Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .pointerInput(currentDoc.id) {
                    // Natural 1-finger pan and 2-finger pinch zoom around gesture midpoint
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (oldScale * zoom).coerceIn(0.25f, 4.0f)

                        // Keep world point under centroid approximately stationary
                        panOffset = centroid - (centroid - panOffset) * (newScale / oldScale) + pan
                        scale = newScale

                        // Debounce camera coordinates to document
                        currentDoc = currentDoc.copy(zoom = scale, panX = panOffset.x, panY = panOffset.y)
                    }
                }
        ) {
            val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            val connectorColor = MaterialTheme.colorScheme.primary

            // Background Canvas: Subtle Graph-Paper Grid and Connectors
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw Graph-Paper Lines (Subtle horizontal & vertical lines)
                if (isGridVisible) {
                    val step = 32f * scale
                    val startX = (panOffset.x % step + step) % step
                    val startY = (panOffset.y % step + step) % step

                    var x = startX
                    while (x < canvasWidth) {
                        drawLine(
                            color = gridLineColor,
                            start = Offset(x, 0f),
                            end = Offset(x, canvasHeight),
                            strokeWidth = 1f
                        )
                        x += step
                    }

                    var y = startY
                    while (y < canvasHeight) {
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(canvasWidth, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }
                }

                // Draw Directional Connectors & Arrowheads
                currentDoc.connectors.forEach { conn ->
                    val fromNode = currentDoc.nodes.firstOrNull { it.id == conn.fromNodeId }
                    val toNode = currentDoc.nodes.firstOrNull { it.id == conn.toNodeId }

                    if (fromNode != null && toNode != null) {
                        val fromCenter = Offset(
                            x = panOffset.x + (fromNode.x + fromNode.width / 2f) * scale,
                            y = panOffset.y + (fromNode.y + fromNode.height / 2f) * scale
                        )
                        val toCenter = Offset(
                            x = panOffset.x + (toNode.x + toNode.width / 2f) * scale,
                            y = panOffset.y + (toNode.y + toNode.height / 2f) * scale
                        )

                        drawLine(
                            color = connectorColor,
                            start = fromCenter,
                            end = toCenter,
                            strokeWidth = (2.5f * scale).coerceIn(1.5f, 4f)
                        )

                        // Arrowhead
                        val angle = atan2(toCenter.y - fromCenter.y, toCenter.x - fromCenter.x)
                        val arrowLength = 14f * scale
                        val p1 = Offset(
                            (toCenter.x - arrowLength * cos(angle - Math.PI / 6)).toFloat(),
                            (toCenter.y - arrowLength * sin(angle - Math.PI / 6)).toFloat()
                        )
                        val p2 = Offset(
                            (toCenter.x - arrowLength * cos(angle + Math.PI / 6)).toFloat(),
                            (toCenter.y - arrowLength * sin(angle + Math.PI / 6)).toFloat()
                        )
                        drawLine(connectorColor, toCenter, p1, strokeWidth = 2.5f * scale)
                        drawLine(connectorColor, toCenter, p2, strokeWidth = 2.5f * scale)
                    }
                }
            }

            // Canvas Objects Layer
            currentDoc.nodes.forEach { node ->
                val isSelected = node.id == selectedNodeId
                val screenX = panOffset.x + node.x * scale
                val screenY = panOffset.y + node.y * scale
                val screenW = (node.width * scale).coerceAtLeast(60f)
                val screenH = (node.height * scale).coerceAtLeast(36f)

                val nodeShape = when (node.shapeType) {
                    "circle" -> CircleShape
                    "pill" -> RoundedCornerShape(percent = 50)
                    "diamond" -> RoundedCornerShape(12.dp)
                    "rect" -> RoundedCornerShape(4.dp)
                    else -> RoundedCornerShape(12.dp)
                }

                Surface(
                    shape = nodeShape,
                    color = getCardBackground(node),
                    tonalElevation = if (isSelected) 8.dp else 2.dp,
                    shadowElevation = if (isSelected) 8.dp else 2.dp,
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    },
                    modifier = Modifier
                        .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
                        .size(screenW.dp, screenH.dp)
                        .pointerInput(node.id, scale, isSnapToGrid) {
                            detectDragGestures(
                                onDragStart = {
                                    selectedNodeId = node.id
                                    pushUndo()
                                },
                                onDragEnd = { canvasStore.save(currentDoc) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dx = dragAmount.x / scale
                                    val dy = dragAmount.y / scale
                                    val updated = currentDoc.nodes.map {
                                        if (it.id == node.id) {
                                            var newX = it.x + dx
                                            var newY = it.y + dy
                                            if (isSnapToGrid) {
                                                newX = (round(newX / 20f) * 20f)
                                                newY = (round(newY / 20f) * 20f)
                                            }
                                            it.copy(x = newX, y = newY)
                                        } else it
                                    }
                                    currentDoc = currentDoc.copy(nodes = updated)
                                }
                            )
                        }
                        .clickable { selectedNodeId = if (isSelected) null else node.id }
                        .padding(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Card Inner Content
                        if (node.type == CanvasNodeType.IMAGE && node.mediaPath.isNotBlank()) {
                            val bmp = remember(node.mediaPath) {
                                try { BitmapFactory.decodeFile(node.mediaPath) } catch (_: Exception) { null }
                            }
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = node.title,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { activeMediaPreview = node }
                                )
                            }
                        } else if (node.type == CanvasNodeType.VIDEO && node.mediaPath.isNotBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { activeMediaPreview = node },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("🎬", fontSize = 24.sp)
                                Text(node.title.ifBlank { "Video" }, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                Text("▶ Tap to play", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        } else if (node.type == CanvasNodeType.NOTE) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { if (node.noteId != null && onOpenNote != null) onOpenNote(node.noteId) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("📝", fontSize = 20.sp)
                                Text(node.title.ifBlank { "Note" }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                if (node.content.isNotBlank()) {
                                    Text(node.content, style = MaterialTheme.typography.labelSmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else if (node.type == CanvasNodeType.FOLDER) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { if (!node.folderName.isNullOrBlank() && onOpenFolder != null) onOpenFolder(node.folderName) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("📁", fontSize = 20.sp)
                                Text(node.title.ifBlank { "Folder" }, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        } else {
                            // Text, Sticky, Flowchart Node
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = node.title.ifBlank { node.type.name },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (node.content.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = node.content,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Resize Handle on Bottom Right (when selected)
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
                                                val updated = currentDoc.nodes.map {
                                                    if (it.id == node.id) {
                                                        it.copy(
                                                            width = (it.width + dw).coerceAtLeast(80f),
                                                            height = (it.height + dh).coerceAtLeast(40f)
                                                        )
                                                    } else it
                                                }
                                                currentDoc = currentDoc.copy(nodes = updated)
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
            }

            // Quick Recenter Action Button
            FilledTonalIconButton(
                onClick = {
                    scale = 1.0f
                    panOffset = Offset(40f, 40f)
                    currentDoc = currentDoc.copy(zoom = 1.0f, panX = 40f, panY = 40f)
                    canvasStore.save(currentDoc)
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Recenter View")
            }
        }
    }

    // Edit Node Dialog
    if (showEditNodeDialog && selectedNode != null) {
        val targetNode = selectedNode
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
                        label = { Text("Content / Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Accent Tint:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 1, 2, 3, 4, 5).forEach { idx ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(getPaletteColor(idx))
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
                        val updated = currentDoc.nodes.map {
                            if (it.id == targetNode.id) {
                                it.copy(title = editTitle.trim(), content = editContent.trim(), colorIndex = editColorIdx)
                            } else it
                        }
                        commitDocChange(currentDoc.copy(nodes = updated))
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

    // Connect Node Dialog
    if (showConnectDialog && connectSourceNodeId != null) {
        val src = currentDoc.nodes.firstOrNull { it.id == connectSourceNodeId }
        val targets = currentDoc.nodes.filter { it.id != connectSourceNodeId }

        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = { Text("Connect to...") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("From: ${src?.title?.ifBlank { src?.type?.name }}")
                    Text("Select target card:", style = MaterialTheme.typography.labelSmall)
                    targets.forEach { target ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val conn = CanvasConnector(
                                        fromNodeId = connectSourceNodeId!!,
                                        toNodeId = target.id,
                                        label = if (src?.type == CanvasNodeType.DECISION) "Next" else ""
                                    )
                                    commitDocChange(currentDoc.copy(connectors = currentDoc.connectors + conn))
                                    showConnectDialog = false
                                }
                                .padding(10.dp)
                        ) {
                            Text(target.title.ifBlank { target.type.name }, fontWeight = FontWeight.Medium)
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

    // ADD CONTENT MODAL BOTTOM SHEET
    if (showAddContentSheet) {
        var pickerMode by remember { mutableStateOf("MENU") } // "MENU", "PICK_NOTE", "PICK_FOLDER"

        ModalBottomSheet(onDismissRequest = { showAddContentSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                if (pickerMode == "MENU") {
                    Text("Add to Canvas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))

                    ListItem(
                        headlineContent = { Text("Sticky Note") },
                        supportingContent = { Text("Quick colored idea card") },
                        leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                        modifier = Modifier.clickable {
                            val n = CanvasNode(
                                type = CanvasNodeType.STICKY,
                                title = "New Idea",
                                x = (240f - panOffset.x) / scale,
                                y = (240f - panOffset.y) / scale,
                                width = 140f,
                                height = 70f,
                                colorIndex = 1
                            )
                            commitDocChange(currentDoc.copy(nodes = currentDoc.nodes + n))
                            selectedNodeId = n.id
                            showAddContentSheet = false
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Link Existing Note") },
                        supportingContent = { Text("Place a card referencing a note in your workspace") },
                        leadingContent = { Icon(Icons.Default.AccountBox, contentDescription = null) },
                        modifier = Modifier.clickable { pickerMode = "PICK_NOTE" }
                    )

                    ListItem(
                        headlineContent = { Text("Link Folder") },
                        supportingContent = { Text("Place a folder card leading to Library") },
                        leadingContent = { Icon(NoteyIcons.Folder, contentDescription = null) },
                        modifier = Modifier.clickable { pickerMode = "PICK_FOLDER" }
                    )

                    ListItem(
                        headlineContent = { Text("Add Photo / Image") },
                        supportingContent = { Text("Insert resizable image card") },
                        leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                        modifier = Modifier.clickable {
                            pendingMediaType = "image"
                            showAddContentSheet = false
                            mediaPickerLauncher.launch(arrayOf("image/*"))
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Browse 600+ Visual Assets") },
                        supportingContent = { Text("Offline vector icons, logos, diagrams & shapes") },
                        leadingContent = { Icon(Icons.Default.Star, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showAddContentSheet = false
                            showAssetPicker = true
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Flowchart / Workflow Node") },
                        supportingContent = { Text("Add Process, Decision, or Terminal node") },
                        leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                        modifier = Modifier.clickable {
                            val n = CanvasNode(
                                type = CanvasNodeType.TASK,
                                title = "Process Step",
                                x = (240f - panOffset.x) / scale,
                                y = (240f - panOffset.y) / scale,
                                width = 150f,
                                height = 60f,
                                shapeType = "rounded"
                            )
                            commitDocChange(currentDoc.copy(nodes = currentDoc.nodes + n))
                            selectedNodeId = n.id
                            showAddContentSheet = false
                        }
                    )
                } else if (pickerMode == "PICK_NOTE" && noteStore != null) {
                    val allNotes = remember { noteStore.loadAll() }
                    Text("Select Note to Link", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(allNotes, key = { it.id }) { note ->
                            ListItem(
                                headlineContent = { Text(note.title.ifBlank { "Untitled Note" }) },
                                supportingContent = { Text(note.body.take(60), maxLines = 1) },
                                modifier = Modifier.clickable {
                                    val newNode = CanvasNode(
                                        type = CanvasNodeType.NOTE,
                                        title = note.title.ifBlank { "Untitled Note" },
                                        content = note.body.take(80),
                                        noteId = note.id,
                                        x = (200f - panOffset.x) / scale,
                                        y = (200f - panOffset.y) / scale,
                                        width = 160f,
                                        height = 80f
                                    )
                                    commitDocChange(currentDoc.copy(nodes = currentDoc.nodes + newNode))
                                    selectedNodeId = newNode.id
                                    showAddContentSheet = false
                                }
                            )
                        }
                    }
                } else if (pickerMode == "PICK_FOLDER" && noteStore != null) {
                    val folders = remember { noteStore.loadAll().map { it.folder }.filter { it.isNotBlank() }.distinct() }
                    Text("Select Folder to Link", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (folders.isEmpty()) {
                        Text("No folders found. Create folders in Notes or Library first.", color = MaterialTheme.colorScheme.outline)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                            items(folders) { folder ->
                                ListItem(
                                    headlineContent = { Text(folder) },
                                    leadingContent = { Icon(NoteyIcons.Folder, contentDescription = null) },
                                    modifier = Modifier.clickable {
                                        val newNode = CanvasNode(
                                            type = CanvasNodeType.FOLDER,
                                            title = folder,
                                            folderName = folder,
                                            x = (200f - panOffset.x) / scale,
                                            y = (200f - panOffset.y) / scale,
                                            width = 150f,
                                            height = 70f
                                        )
                                        commitDocChange(currentDoc.copy(nodes = currentDoc.nodes + newNode))
                                        selectedNodeId = newNode.id
                                        showAddContentSheet = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Visual Asset Picker Sheet
    if (showAssetPicker) {
        AssetPickerSheet(
            onDismiss = { showAssetPicker = false },
            onAssetSelected = { asset ->
                val newNode = CanvasNode(
                    type = CanvasNodeType.ASSET,
                    title = asset.name,
                    assetId = asset.id,
                    x = (220f - panOffset.x) / scale,
                    y = (220f - panOffset.y) / scale,
                    width = 110f,
                    height = 110f,
                    shapeType = "rounded"
                )
                commitDocChange(currentDoc.copy(nodes = currentDoc.nodes + newNode))
                selectedNodeId = newNode.id
                showAssetPicker = false
            }
        )
    }

    // Media Popup Player Dialog
    if (activeMediaPreview != null) {
        val node = activeMediaPreview!!
        Dialog(onDismissRequest = { activeMediaPreview = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(node.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        IconButton(onClick = { activeMediaPreview = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (node.type == CanvasNodeType.IMAGE && node.mediaPath.isNotBlank()) {
                        val bmp = BitmapFactory.decodeFile(node.mediaPath)
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp).clip(RoundedCornerShape(8.dp))
                            )
                        }
                    } else if (node.type == CanvasNodeType.VIDEO && node.mediaPath.isNotBlank()) {
                        AndroidView(
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    setVideoURI(Uri.fromFile(File(node.mediaPath)))
                                    val mc = MediaController(ctx)
                                    mc.setAnchorView(this)
                                    setMediaController(mc)
                                    start()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(260.dp)
                        )
                    }
                }
            }
        }
    }

    // Image Editor Dialog
    if (imageToEditPath != null) {
        ImageEditorDialog(
            imagePath = imageToEditPath!!,
            onDismiss = { imageToEditPath = null },
            onSaveSuccess = { newPath ->
                val updated = currentDoc.nodes.map {
                    if (it.mediaPath == imageToEditPath) it.copy(mediaPath = newPath) else it
                }
                commitDocChange(currentDoc.copy(nodes = updated))
                imageToEditPath = null
            }
        )
    }
}

private fun getCardBackground(node: CanvasNode): Color {
    return when (node.colorIndex) {
        1 -> Color(0xFFEFF6FF)
        2 -> Color(0xFFECFDF5)
        3 -> Color(0xFFFEF3C7)
        4 -> Color(0xFFFAF5FF)
        5 -> Color(0xFFFFF1F2)
        else -> Color(0xFFF8FAFC)
    }
}

private fun getPaletteColor(idx: Int): Color {
    return when (idx) {
        1 -> Color(0xFF3B82F6)
        2 -> Color(0xFF10B981)
        3 -> Color(0xFFF59E0B)
        4 -> Color(0xFF8B5CF6)
        5 -> Color(0xFFEF4444)
        else -> Color(0xFF64748B)
    }
}

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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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

    val density = LocalDensity.current
    val isDark = isSystemInDarkTheme()

    // Viewport & Zoom State
    var scale by remember(currentDoc.id) { mutableStateOf(currentDoc.zoom.coerceIn(0.25f, 3.5f)) }
    var panOffset by remember(currentDoc.id) { mutableStateOf(Offset(currentDoc.panX, currentDoc.panY)) }

    // Active Selection State
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    val selectedNode = currentDoc.nodes.firstOrNull { it.id == selectedNodeId }

    // Dialog & Sheet Controls
    var showAddContentSheet by remember { mutableStateOf(false) }
    var showEditNodeDialog by remember { mutableStateOf(false) }
    var showRenameDocDialog by remember { mutableStateOf(false) }
    var showConnectDialog by remember { mutableStateOf(false) }
    var connectSourceNodeId by remember { mutableStateOf<String?>(null) }
    var activeMediaPreview by remember { mutableStateOf<CanvasNode?>(null) }
    var imageToEditPath by remember { mutableStateOf<String?>(null) }

    // Grid Preferences (Local toggle overrides)
    var isGridVisible by remember { mutableStateOf(settings.canvasGrid) }
    var isSnapToGrid by remember { mutableStateOf(settings.canvasSnapToGrid) }

    // Device Image / Logo Upload Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
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
                    content = if (isVid) "Video Card" else "Custom Image / Logo",
                    x = (200f - panOffset.x) / scale,
                    y = (200f - panOffset.y) / scale,
                    width = 180f,
                    height = 140f,
                    mediaPath = att.filePath,
                    mediaType = if (isVid) "video" else "image"
                )
                commitDocChange(currentDoc.copy(nodes = currentDoc.nodes + newNode))
                selectedNodeId = newNode.id
                Toast.makeText(context, "Image added to canvas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NoteySurfaces.Surface0)
    ) {


        // Touch-First Infinite Canvas Area with strict boundary clipping
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(currentDoc.id) {
                    // Natural 1-finger pan and 2-finger pinch zoom around gesture midpoint
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val oldScale = scale
                        val newScale = (oldScale * zoom).coerceIn(0.25f, 3.5f)

                        // Keep world point under centroid approximately stationary
                        panOffset = centroid - (centroid - panOffset) * (newScale / oldScale) + pan
                        scale = newScale

                        // Debounce camera coordinates to document
                        currentDoc = currentDoc.copy(zoom = scale, panX = panOffset.x, panY = panOffset.y)
                    }
                }
        ) {
            val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.18f else 0.25f)
            val connectorColor = if (isDark) Color(0xFF8AB4F8) else MaterialTheme.colorScheme.primary

            // Background Canvas: #27272A Radial Dot Grid (24dp spacing) and SVG Cubic Bezier Connectors
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Radial Dot Grid with #27272A (24dp spacing)
                if (isGridVisible) {
                    val dotSpacing = with(density) { 24.dp.toPx() } * scale
                    if (dotSpacing > 8f) {
                        val startX = (panOffset.x % dotSpacing + dotSpacing) % dotSpacing
                        val startY = (panOffset.y % dotSpacing + dotSpacing) % dotSpacing
                        val dotColor = Color(0xFF27272A)
                        val dotRadius = (1.2f * scale).coerceIn(0.8f, 2.4f)

                        var x = startX
                        while (x < canvasWidth) {
                            var y = startY
                            while (y < canvasHeight) {
                                drawCircle(
                                    color = dotColor,
                                    radius = dotRadius,
                                    center = Offset(x, y)
                                )
                                y += dotSpacing
                            }
                            x += dotSpacing
                        }
                    }
                }

                // Connectors: SVG Cubic Bezier curves (#3F3F46, dashed 4 4)
                currentDoc.connectors.forEach { conn ->
                    val fromNode = currentDoc.nodes.firstOrNull { it.id == conn.fromNodeId }
                    val toNode = currentDoc.nodes.firstOrNull { it.id == conn.toNodeId }

                    if (fromNode != null && toNode != null && fromNode.id != toNode.id) {
                        val p1 = Offset(
                            x = panOffset.x + (fromNode.x + fromNode.width) * scale,
                            y = panOffset.y + (fromNode.y + fromNode.height / 2f) * scale
                        )
                        val p2 = Offset(
                            x = panOffset.x + toNode.x * scale,
                            y = panOffset.y + (toNode.y + toNode.height / 2f) * scale
                        )

                        val dx = (p2.x - p1.x).coerceAtLeast(30f) * 0.5f
                        val ctrl1 = Offset(p1.x + dx, p1.y)
                        val ctrl2 = Offset(p2.x - dx, p2.y)

                        val bezierPath = Path().apply {
                            moveTo(p1.x, p1.y)
                            cubicTo(ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, p2.x, p2.y)
                        }

                        drawPath(
                            path = bezierPath,
                            color = Color(0xFF3F3F46),
                            style = Stroke(
                                width = (2f * scale).coerceIn(1.5f, 3.5f),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                        )

                        // Arrowhead indicator at destination
                        drawCircle(
                            color = Color(0xFF71717A),
                            radius = (3f * scale).coerceIn(2.5f, 5f),
                            center = p2
                        )
                    }
                }
            }

            // Canvas Objects Layer (Pixel-perfect scale & world transform)
            currentDoc.nodes.forEach { node ->
                val isSelected = node.id == selectedNodeId
                val screenX = panOffset.x + node.x * scale
                val screenY = panOffset.y + node.y * scale
                val screenWidthDp = with(density) { ((node.width * scale).coerceAtLeast(60f)).toDp() }
                val screenHeightDp = with(density) { ((node.height * scale).coerceAtLeast(36f)).toDp() }

                val nodeShape = RoundedCornerShape(8.dp)
                val nodeAccent = GoogleKeepPalette.getAccentColor(node.colorIndex)
                val invertedStatusText = getInvertedTextColor(nodeAccent)

                Surface(
                    shape = nodeShape,
                    color = NoteySurfaces.Surface1,
                    tonalElevation = if (isSelected) 8.dp else 2.dp,
                    shadowElevation = if (isSelected) 6.dp else 2.dp,
                    border = if (isSelected) {
                        androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder)
                    },
                    modifier = Modifier
                        .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
                        .size(screenWidthDp, screenHeightDp)
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
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // 3.5dp dynamic accent left border
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .fillMaxHeight()
                                .background(nodeAccent)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(8.dp)
                        ) {
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
                                            .clip(RoundedCornerShape(6.dp))
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
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    Text(
                                        text = node.title.ifBlank { "Video" },
                                        style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = NoteySurfaces.TextPrimary),
                                        maxLines = 1
                                    )
                                }
                            } else if (node.type == CanvasNodeType.NOTE) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { if (node.noteId != null && onOpenNote != null) onOpenNote(node.noteId) },
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(NoteyIcons.Document, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = node.title.ifBlank { "Note" },
                                            style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NoteySurfaces.TextPrimary),
                                            maxLines = 1
                                        )
                                    }
                                    if (node.content.isNotBlank()) {
                                        Text(
                                            text = node.content,
                                            style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 10.sp, color = NoteySurfaces.TextSecondary),
                                            maxLines = 1
                                        )
                                    }
                                }
                            } else if (node.type == CanvasNodeType.FOLDER) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { if (!node.folderName.isNullOrBlank() && onOpenFolder != null) onOpenFolder(node.folderName) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(NoteyIcons.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = node.title.ifBlank { "Folder" },
                                        style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NoteySurfaces.TextPrimary),
                                        maxLines = 1
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.Start,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    // Status pill with automatic contrast inversion
                                    Surface(
                                        shape = RoundedCornerShape(3.dp),
                                        color = nodeAccent
                                    ) {
                                        Text(
                                            text = node.type.name,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = invertedStatusText
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = node.title.ifBlank { node.type.name },
                                        style = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NoteySurfaces.TextPrimary),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (node.content.isNotBlank()) {
                                        Text(
                                            text = node.content,
                                            style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 10.sp, color = NoteySurfaces.TextSecondary),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Resize handle on bottom right (when selected)
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(16.dp)
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
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating Node Contextual Action Bar: Anchored DIRECTLY ABOVE selected node
        if (selectedNode != null) {
            val node = selectedNode
            val nodeScrX = panOffset.x + (node.x * scale)
            val nodeScrY = panOffset.y + (node.y * scale) - with(density) { 46.dp.toPx() }
            val clampedX = nodeScrX.coerceIn(12f, 700f)
            val clampedY = nodeScrY.coerceIn(70f, 1800f)

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NoteySurfaces.Surface2,
                border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorderStrong),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .offset { IntOffset(clampedX.roundToInt(), clampedY.roundToInt()) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = { showEditNodeDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(NoteyIcons.Pencil, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit & Files", fontSize = 11.sp, color = NoteySurfaces.TextPrimary)
                    }

                    TextButton(
                        onClick = {
                            val cloned = node.copy(
                                id = UUID.randomUUID().toString(),
                                x = node.x + 30f,
                                y = node.y + 30f,
                                title = if (node.title.isNotBlank()) "${node.title} (Copy)" else ""
                            )
                            commitDocChange(currentDoc.copy(nodes = currentDoc.nodes + cloned))
                            selectedNodeId = cloned.id
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Duplicate", fontSize = 11.sp, color = NoteySurfaces.TextPrimary)
                    }

                    TextButton(
                        onClick = {
                            connectSourceNodeId = node.id
                            showConnectDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Connect", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = {
                            commitDocChange(currentDoc.copy(
                                nodes = currentDoc.nodes.filter { it.id != node.id },
                                connectors = currentDoc.connectors.filter { it.fromNodeId != node.id && it.toNodeId != node.id }
                            ))
                            selectedNodeId = null
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Floating Header Pill: Rounded capsule with back button, editable title, integrated zoom controls ([-] 100% [+]) with single-tap Recenter
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = NoteySurfaces.Surface2,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorderStrong),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 12.dp, end = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NoteySurfaces.TextPrimary, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Editable Title
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { showRenameDocDialog = true }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentDoc.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NoteySurfaces.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 130.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Undo & Redo
                IconButton(
                    onClick = { applyUndo() },
                    enabled = undoStack.isNotEmpty(),
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(
                        "<",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = if (undoStack.isNotEmpty()) NoteySurfaces.TextPrimary else NoteySurfaces.TextMuted
                    )
                }

                IconButton(
                    onClick = { applyRedo() },
                    enabled = redoStack.isNotEmpty(),
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(
                        ">",
                        style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp),
                        color = if (redoStack.isNotEmpty()) NoteySurfaces.TextPrimary else NoteySurfaces.TextMuted
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Integrated Zoom Controls: [-] 100% [+]
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NoteySurfaces.Surface3,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { scale = (scale - 0.2f).coerceAtLeast(0.25f) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", color = NoteySurfaces.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Text(
                            text = "${(scale * 100).roundToInt()}%",
                            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NoteySurfaces.TextPrimary),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { scale = (scale + 0.2f).coerceAtMost(3.5f) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = NoteySurfaces.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Single-tap Recenter
                IconButton(
                    onClick = {
                        scale = 1.0f
                        panOffset = Offset(0f, 0f)
                        commitDocChange(currentDoc.copy(zoom = 1.0f, panX = 0f, panY = 0f))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(NoteyIcons.Recenter, contentDescription = "Recenter", tint = NoteySurfaces.TextSecondary, modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Add Object Button
                IconButton(
                    onClick = { showAddContentSheet = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Object", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Live Radar Minimap: In bottom-right corner tracking node coordinates
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = NoteySurfaces.Surface2.copy(alpha = 0.9f),
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorderStrong),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(64.dp, 48.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                val mw = size.width
                val mh = size.height

                val minX = (currentDoc.nodes.minOfOrNull { it.x } ?: 0f).coerceAtMost(0f)
                val maxX = (currentDoc.nodes.maxOfOrNull { it.x + it.width } ?: 1000f).coerceAtLeast(1000f)
                val minY = (currentDoc.nodes.minOfOrNull { it.y } ?: 0f).coerceAtMost(0f)
                val maxY = (currentDoc.nodes.maxOfOrNull { it.y + it.height } ?: 800f).coerceAtLeast(800f)

                val spanX = (maxX - minX).coerceAtLeast(100f)
                val spanY = (maxY - minY).coerceAtLeast(100f)

                currentDoc.nodes.forEach { n ->
                    val nx = ((n.x - minX) / spanX) * mw
                    val ny = ((n.y - minY) / spanY) * mh
                    val isSel = n.id == selectedNodeId
                    drawCircle(
                        color = if (isSel) Color(0xFF38BDF8) else Color(0xFF71717A),
                        radius = if (isSel) 2.5f else 1.5f,
                        center = Offset(nx, ny)
                    )
                }

                // Camera viewport indicator
                val camX = ((-panOffset.x / scale - minX) / spanX) * mw
                val camY = ((-panOffset.y / scale - minY) / spanY) * mh
                val camW = ((size.width * 2f / scale) / spanX) * mw
                val camH = ((size.height * 2f / scale) / spanY) * mh

                drawRect(
                    color = Color(0x4438BDF8),
                    topLeft = Offset(camX.coerceIn(0f, mw), camY.coerceIn(0f, mh)),
                    size = Size(camW.coerceIn(4f, mw), camH.coerceIn(4f, mh)),
                    style = Stroke(width = 1f)
                )
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

    // Rename Canvas Document Dialog
    if (showRenameDocDialog) {
        var newDocName by remember(currentDoc.name) { mutableStateOf(currentDoc.name) }
        AlertDialog(
            onDismissRequest = { showRenameDocDialog = false },
            title = { Text("Rename Canvas") },
            text = {
                OutlinedTextField(
                    value = newDocName,
                    onValueChange = { newDocName = it },
                    label = { Text("Canvas Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newDocName.isNotBlank()) {
                        commitDocChange(currentDoc.copy(name = newDocName.trim()))
                    }
                    showRenameDocDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDocDialog = false }) {
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
                                colorIndex = 3
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
                        headlineContent = { Text("Upload Image / Custom Logo") },
                        supportingContent = { Text("Choose a photo, logo, or icon from your device") },
                        leadingContent = { Icon(Icons.Default.Add, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showAddContentSheet = false
                            imagePickerLauncher.launch(arrayOf("image/*"))
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Flowchart / Workflow Step") },
                        supportingContent = { Text("Add Process step or task node") },
                        leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                        modifier = Modifier.clickable {
                            val n = CanvasNode(
                                type = CanvasNodeType.TASK,
                                title = "Process Step",
                                x = (240f - panOffset.x) / scale,
                                y = (240f - panOffset.y) / scale,
                                width = 160f,
                                height = 60f,
                                shapeType = "rounded"
                            )
                            commitDocChange(currentDoc.copy(nodes = currentDoc.nodes + n))
                            selectedNodeId = n.id
                            showAddContentSheet = false
                        }
                    )

                    ListItem(
                        headlineContent = { Text("Decision Node") },
                        supportingContent = { Text("Add conditional diamond branch") },
                        leadingContent = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        modifier = Modifier.clickable {
                            val n = CanvasNode(
                                type = CanvasNodeType.DECISION,
                                title = "Decision?",
                                x = (240f - panOffset.x) / scale,
                                y = (240f - panOffset.y) / scale,
                                width = 140f,
                                height = 70f,
                                shapeType = "diamond",
                                colorIndex = 1
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
                                        height = 80f,
                                        colorIndex = note.colorIndex
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

private fun getCanvasNodeBg(node: CanvasNode, isDark: Boolean): Color {
    return if (isDark) {
        when (node.colorIndex) {
            1 -> Color(0xFF1E3A8A) // Dark Blue
            2 -> Color(0xFF064E3B) // Dark Green
            3 -> Color(0xFF78350F) // Dark Amber / Yellow
            4 -> Color(0xFF581C87) // Dark Purple
            5 -> Color(0xFF881337) // Dark Rose
            else -> Color(0xFF1E293B) // Slate Dark
        }
    } else {
        when (node.colorIndex) {
            1 -> Color(0xFFEFF6FF) // Light Blue
            2 -> Color(0xFFECFDF5) // Light Green
            3 -> Color(0xFFFEF3C7) // Light Amber / Cream
            4 -> Color(0xFFFAF5FF) // Light Purple
            5 -> Color(0xFFFFF1F2) // Light Rose
            else -> Color(0xFFFFFFFF) // Crisp White
        }
    }
}

private fun getCanvasNodeTextColor(node: CanvasNode, isDark: Boolean): Color {
    return if (isDark) Color(0xFFF1F5F9) else Color(0xFF0F172A)
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

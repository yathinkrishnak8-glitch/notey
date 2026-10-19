package com.pocketdev.notey

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File
import java.text.DateFormat
import java.util.Date

enum class NavDestination {
    HOME, NOTES, CANVAS, LIBRARY, SETTINGS
}

enum class ScreenState {
    SPLASH, PROFILE_SETUP, MAIN, NOTE_READER, NOTE_EDITOR, SEARCH, LOGO_STUDIO
}

class MainActivity : ComponentActivity() {
    private lateinit var noteStore: NoteStore
    private lateinit var canvasStore: CanvasStore
    private lateinit var settingsStore: SettingsStore
    private lateinit var userProfileStore: UserProfileStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        noteStore = NoteStore(applicationContext)
        canvasStore = CanvasStore(applicationContext)
        settingsStore = SettingsStore(applicationContext)
        userProfileStore = UserProfileStore(applicationContext)

        setContent {
            var settings by remember { mutableStateOf(settingsStore.loadSettings()) }
            var userProfile by remember { mutableStateOf(userProfileStore.getProfile()) }

            NoteyTheme(settings = settings) {
                NoteyApp(
                    noteStore = noteStore,
                    canvasStore = canvasStore,
                    settingsStore = settingsStore,
                    userProfileStore = userProfileStore,
                    settings = settings,
                    userProfile = userProfile,
                    onUpdateSettings = {
                        settings = it
                        settingsStore.saveSettings(it)
                    },
                    onUpdateProfile = {
                        userProfile = it
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteyApp(
    noteStore: NoteStore,
    canvasStore: CanvasStore,
    settingsStore: SettingsStore,
    userProfileStore: UserProfileStore,
    settings: AppSettings,
    userProfile: UserProfile?,
    onUpdateSettings: (AppSettings) -> Unit,
    onUpdateProfile: (UserProfile) -> Unit
) {
    var screenState by remember {
        mutableStateOf(if (userProfile != null) ScreenState.MAIN else ScreenState.PROFILE_SETUP)
    }
    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
    var activeNoteId by remember { mutableStateOf<Long?>(null) }
    var showQuickCreateSheet by remember { mutableStateOf(false) }
    var canvasDocIdToOpen by remember { mutableStateOf<String?>(null) }
    var searchScopeFilter by remember { mutableStateOf("All") }

    val animDuration = if (!settings.animationsEnabled || settings.reduceMotion) 0 else settings.animationSpeed.durationMs

    // Handle system back button
    BackHandler(enabled = screenState != ScreenState.MAIN) {
        when (screenState) {
            ScreenState.NOTE_READER, ScreenState.NOTE_EDITOR, ScreenState.SEARCH, ScreenState.LOGO_STUDIO -> {
                screenState = ScreenState.MAIN
            }
            ScreenState.PROFILE_SETUP -> {
                // If profile is not yet configured, back exits or stays on profile setup
            }
            ScreenState.SPLASH -> {
                // Let system exit
            }
            else -> {}
        }
    }

    AnimatedContent(
        targetState = screenState,
        transitionSpec = {
            if (animDuration == 0) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                fadeIn(animationSpec = tween(animDuration)) togetherWith
                        fadeOut(animationSpec = tween(animDuration))
            }
        },
        label = "ScreenTransition"
    ) { state ->
        when (state) {
            ScreenState.SPLASH -> {
                SplashScreen(onFinish = {
                    screenState = if (userProfile != null) ScreenState.MAIN else ScreenState.PROFILE_SETUP
                })
            }
            ScreenState.PROFILE_SETUP -> {
                ProfileSetupScreen(
                    userProfileStore = userProfileStore,
                    onProfileSetupComplete = { createdProfile ->
                        onUpdateProfile(createdProfile)
                        screenState = ScreenState.MAIN
                    }
                )
            }
            ScreenState.NOTE_READER -> {
                NoteReaderView(
                    noteId = activeNoteId ?: 0L,
                    noteStore = noteStore,
                    onBack = { screenState = ScreenState.MAIN },
                    onEdit = { screenState = ScreenState.NOTE_EDITOR }
                )
            }
            ScreenState.NOTE_EDITOR -> {
                NoteEditorView(
                    noteId = activeNoteId,
                    noteStore = noteStore,
                    settings = settings,
                    onBack = { screenState = ScreenState.MAIN },
                    onSaved = { savedId ->
                        activeNoteId = savedId
                        screenState = ScreenState.NOTE_READER
                    }
                )
            }
            ScreenState.SEARCH -> {
                GlobalSearchScreen(
                    noteStore = noteStore,
                    canvasStore = canvasStore,
                    initialScope = searchScopeFilter,
                    onOpenNote = { id ->
                        activeNoteId = id
                        screenState = ScreenState.NOTE_READER
                    },
                    onOpenCanvas = { cid ->
                        canvasDocIdToOpen = cid
                        currentDestination = NavDestination.CANVAS
                        screenState = ScreenState.MAIN
                    },
                    onBack = { screenState = ScreenState.MAIN }
                )
            }
            ScreenState.LOGO_STUDIO -> {
                LogoStudioScreen(
                    onBack = { screenState = ScreenState.MAIN }
                )
            }
            ScreenState.MAIN -> {
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 3.dp
                        ) {
                            NavigationBarItem(
                                selected = currentDestination == NavDestination.HOME,
                                onClick = { currentDestination = NavDestination.HOME },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = currentDestination == NavDestination.NOTES,
                                onClick = { currentDestination = NavDestination.NOTES },
                                icon = { Icon(Icons.Default.Edit, contentDescription = "Notes") },
                                label = { Text("Notes") }
                            )
                            NavigationBarItem(
                                selected = currentDestination == NavDestination.CANVAS,
                                onClick = {
                                    canvasDocIdToOpen = null
                                    currentDestination = NavDestination.CANVAS
                                },
                                icon = { Icon(Icons.Default.Share, contentDescription = "Canvas") },
                                label = { Text("Canvas") }
                            )
                            NavigationBarItem(
                                selected = currentDestination == NavDestination.LIBRARY,
                                onClick = { currentDestination = NavDestination.LIBRARY },
                                icon = { Icon(NoteyIcons.Folder, contentDescription = "Library") },
                                label = { Text("Library") }
                            )
                            NavigationBarItem(
                                selected = currentDestination == NavDestination.SETTINGS,
                                onClick = { currentDestination = NavDestination.SETTINGS },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings") }
                            )
                        }
                    },
                    floatingActionButton = {
                        if (currentDestination in listOf(NavDestination.HOME, NavDestination.NOTES)) {
                            FloatingActionButton(
                                onClick = { showQuickCreateSheet = true },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.White,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Create New")
                            }
                        }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        when (currentDestination) {
                            NavDestination.HOME -> {
                                HomeDashboardView(
                                    noteStore = noteStore,
                                    canvasStore = canvasStore,
                                    userProfile = userProfile,
                                    onSearchClick = {
                                        searchScopeFilter = "All"
                                        screenState = ScreenState.SEARCH
                                    },
                                    onOpenNote = { id ->
                                        activeNoteId = id
                                        screenState = ScreenState.NOTE_READER
                                    },
                                    onOpenCanvas = { cid ->
                                        canvasDocIdToOpen = cid
                                        currentDestination = NavDestination.CANVAS
                                    },
                                    onQuickAction = { action ->
                                        when (action) {
                                            "note" -> {
                                                activeNoteId = null
                                                screenState = ScreenState.NOTE_EDITOR
                                            }
                                            "checklist" -> {
                                                activeNoteId = null
                                                screenState = ScreenState.NOTE_EDITOR
                                            }
                                            "canvas" -> {
                                                canvasDocIdToOpen = null
                                                currentDestination = NavDestination.CANVAS
                                            }
                                            "logo" -> {
                                                screenState = ScreenState.LOGO_STUDIO
                                            }
                                            "folder" -> {
                                                currentDestination = NavDestination.NOTES
                                            }
                                        }
                                    }
                                )
                            }
                            NavDestination.NOTES -> {
                                NotesListView(
                                    noteStore = noteStore,
                                    onOpenNote = { id ->
                                        activeNoteId = id
                                        screenState = ScreenState.NOTE_READER
                                    },
                                    onCreateNote = {
                                        activeNoteId = null
                                        screenState = ScreenState.NOTE_EDITOR
                                    },
                                    onSearchClick = {
                                        searchScopeFilter = "Notes"
                                        screenState = ScreenState.SEARCH
                                    }
                                )
                            }
                            NavDestination.CANVAS -> {
                                CanvasScreen(
                                    canvasStore = canvasStore,
                                    settings = settings,
                                    noteStore = noteStore,
                                    initialDocId = canvasDocIdToOpen,
                                    onOpenNote = { id ->
                                        activeNoteId = id
                                        screenState = ScreenState.NOTE_READER
                                    },
                                    onOpenFolder = { _ ->
                                        currentDestination = NavDestination.LIBRARY
                                    }
                                )
                            }
                            NavDestination.LIBRARY -> {
                                LibraryScreen(
                                    noteStore = noteStore,
                                    onOpenNote = { id ->
                                        activeNoteId = id
                                        screenState = ScreenState.NOTE_READER
                                    }
                                )
                            }
                            NavDestination.SETTINGS -> {
                                SettingsScreen(
                                    settings = settings,
                                    onSettingsChanged = onUpdateSettings,
                                    noteStore = noteStore,
                                    userProfileStore = userProfileStore,
                                    currentProfile = userProfile,
                                    onProfileChanged = onUpdateProfile
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Create Modal Bottom Sheet
    if (showQuickCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickCreateSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create New",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showQuickCreateSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                QuickCreateMenuItem(
                    icon = Icons.Default.Edit,
                    title = "New Note",
                    subtitle = "Write a simple note",
                    onClick = {
                        showQuickCreateSheet = false
                        activeNoteId = null
                        screenState = ScreenState.NOTE_EDITOR
                    }
                )

                QuickCreateMenuItem(
                    icon = Icons.Default.Check,
                    title = "New Checklist",
                    subtitle = "Create a task list",
                    onClick = {
                        showQuickCreateSheet = false
                        activeNoteId = null
                        screenState = ScreenState.NOTE_EDITOR
                    }
                )

                QuickCreateMenuItem(
                    icon = Icons.Default.Share,
                    title = "New Canvas",
                    subtitle = "Visual workspace & flowchart",
                    onClick = {
                        showQuickCreateSheet = false
                        canvasDocIdToOpen = null
                        currentDestination = NavDestination.CANVAS
                    }
                )

                QuickCreateMenuItem(
                    icon = Icons.Default.Star,
                    title = "Logo Studio",
                    subtitle = "Create logos & brand emblems",
                    onClick = {
                        showQuickCreateSheet = false
                        screenState = ScreenState.LOGO_STUDIO
                    }
                )

                QuickCreateMenuItem(
                    icon = NoteyIcons.Folder,
                    title = "New Folder",
                    subtitle = "Organize your notes into collections",
                    onClick = {
                        showQuickCreateSheet = false
                        currentDestination = NavDestination.NOTES
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun QuickCreateMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F071D))
            .clickable(onClick = onFinish),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF9333EA))
                    .shadow(16.dp, RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🪶", fontSize = 50.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Notey",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Capture anything. Organize everything. Connect ideas.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC084FC)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = SettingsStore.DEVELOPER_ATTRIBUTION,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun HomeDashboardView(
    noteStore: NoteStore,
    canvasStore: CanvasStore,
    userProfile: UserProfile?,
    onSearchClick: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenCanvas: (String) -> Unit,
    onQuickAction: (String) -> Unit
) {
    val activeNotes = remember { noteStore.getActiveNotes() }
    val pinnedNotes = remember(activeNotes) { activeNotes.filter { it.isPinned } }
    val recentNotes = remember(activeNotes) { activeNotes.filter { !it.isPinned }.take(5) }
    val canvases = remember { canvasStore.loadAll() }

    val greetingText = remember(userProfile?.displayName) {
        UserProfileValidator.getTimeAwareGreeting(userProfile?.displayName ?: "")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Dynamic Personalized Header with Avatar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greetingText,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Here's what's happening today in your workspace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar Badge
            val avatarBitmap = remember(userProfile?.avatarUri) {
                if (userProfile?.avatarUri != null) {
                    try { BitmapFactory.decodeFile(userProfile.avatarUri) } catch (_: Exception) { null }
                } else null
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = UserProfileValidator.getInitials(userProfile?.displayName ?: "User"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Search Bar trigger
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSearchClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Search notes, files, canvases...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        // Quick Create Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickActionPill(icon = Icons.Default.Edit, label = "Note", onClick = { onQuickAction("note") }, modifier = Modifier.weight(1f))
            QuickActionPill(icon = Icons.Default.Check, label = "Checklist", onClick = { onQuickAction("checklist") }, modifier = Modifier.weight(1f))
            QuickActionPill(icon = Icons.Default.Share, label = "Canvas", onClick = { onQuickAction("canvas") }, modifier = Modifier.weight(1f))
            QuickActionPill(icon = Icons.Default.Star, label = "Logos", onClick = { onQuickAction("logo") }, modifier = Modifier.weight(1f))
        }

        // Pinned Section
        if (pinnedNotes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PINNED",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                pinnedNotes.forEach { note ->
                    NoteCard(
                        note = note,
                        onClick = { onOpenNote(note.id) },
                        onTogglePin = { noteStore.togglePin(note.id) }
                    )
                }
            }
        }

        // Recent Notes Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Recent Notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (recentNotes.isEmpty() && pinnedNotes.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📝", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No notes yet", fontWeight = FontWeight.SemiBold)
                        Text("Tap + or New Note to write your first thought.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                recentNotes.forEach { note ->
                    NoteCard(
                        note = note,
                        onClick = { onOpenNote(note.id) },
                        onTogglePin = { noteStore.togglePin(note.id) }
                    )
                }
            }
        }

        // Recent Canvases & Workflows
        if (canvases.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Canvases & Workflows",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(canvases) { doc ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .width(220.dp)
                                .clickable { onOpenCanvas(doc.id) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = if (doc.type == CanvasType.WORKFLOW) "⚡ Workflow" else "🔀 Canvas",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = doc.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${doc.nodes.size} nodes • ${doc.connectors.size} connections",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun QuickActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListView(
    noteStore: NoteStore,
    onOpenNote: (Long) -> Unit,
    onCreateNote: () -> Unit,
    onSearchClick: () -> Unit
) {
    var notes by remember { mutableStateOf(noteStore.getActiveNotes()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedFolder by remember { mutableStateOf("") }
    var showFolderDialog by remember { mutableStateOf(false) }

    val folders = remember(notes) { noteStore.getFolders() }

    val filteredNotes = remember(notes, selectedFilter, selectedFolder) {
        notes.filter { note ->
            when (selectedFilter) {
                "Recent" -> true
                "Favorites" -> note.isFavorite
                "Checklists" -> note.isChecklist
                "Folder" -> selectedFolder.isBlank() || note.folder.equals(selectedFolder, ignoreCase = true)
                else -> true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App Bar
        Surface(
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search Notes")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Recent", "Favorites", "Checklists").forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter && selectedFolder.isBlank(),
                            onClick = {
                                selectedFilter = filter
                                selectedFolder = ""
                            },
                            label = { Text(filter) }
                        )
                    }

                    if (folders.isNotEmpty()) {
                        folders.forEach { fName ->
                            FilterChip(
                                selected = selectedFolder == fName,
                                onClick = {
                                    selectedFilter = "Folder"
                                    selectedFolder = fName
                                },
                                label = { Text("📁 $fName") }
                            )
                        }
                    }

                    FilterChip(
                        selected = false,
                        onClick = { showFolderDialog = true },
                        label = { Text("+ Folder") }
                    )
                }
            }
        }

        // Notes List
        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📝", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No notes found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Create a note or change your filters.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onOpenNote(note.id) },
                        onTogglePin = {
                            noteStore.togglePin(note.id)
                            notes = noteStore.getActiveNotes()
                        },
                        onToggleFavorite = {
                            noteStore.toggleFavorite(note.id)
                            notes = noteStore.getActiveNotes()
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(72.dp)) }
            }
        }
    }

    if (showFolderDialog) {
        var folderNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameInput.isNotBlank()) {
                            selectedFolder = folderNameInput.trim()
                            selectedFilter = "Folder"
                            showFolderDialog = false
                        }
                    }
                ) {
                    Text("Filter Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onTogglePin: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null
) {
    val accentColor = getNoteColor(note.colorIndex)
    val dateStr = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(note.updatedAt))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onToggleFavorite != null) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(if (note.isFavorite) "★" else "☆", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (onTogglePin != null) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text(if (note.isPinned) "📌" else "📍", fontSize = 14.sp)
                        }
                    }
                }
            }

            if (note.body.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (note.folder.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "📁 ${note.folder}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (note.attachments.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "📎 ${note.attachments.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteReaderView(
    noteId: Long,
    noteStore: NoteStore,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    var note by remember(noteId) { mutableStateOf(noteStore.loadAll().firstOrNull { it.id == noteId }) }

    if (note == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val currentNote = note!!
    val wordCount = remember(currentNote.body) {
        currentNote.body.split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val updatedDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(currentNote.updatedAt))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Note Reader", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, currentNote.title)
                                putExtra(Intent.EXTRA_TEXT, "${currentNote.title}\n\n${currentNote.body}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share note"))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(
                        onClick = {
                            noteStore.togglePin(currentNote.id)
                            note = currentNote.copy(isPinned = !currentNote.isPinned)
                        }
                    ) {
                        Text(if (currentNote.isPinned) "📌" else "📍", fontSize = 18.sp)
                    }
                    IconButton(
                        onClick = {
                            noteStore.permanentlyDelete(currentNote.id)
                            onBack()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onEdit,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Note")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Title
            Text(
                text = currentNote.title.ifBlank { "Untitled Note" },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            // Metadata info bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$wordCount words • Updated $updatedDate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (currentNote.folder.isNotBlank()) {
                    Text(
                        text = "📁 ${currentNote.folder}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider()

            // Note Body
            Text(
                text = currentNote.body,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp
            )

            // Media Attachments
            if (currentNote.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Attachments (${currentNote.attachments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                currentNote.attachments.forEach { att ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (att.type == AttachmentType.IMAGE) "🖼️" else if (att.type == AttachmentType.VIDEO) "🎬" else "📄", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(att.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${att.sizeBytes / 1024} KB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorView(
    noteId: Long?,
    noteStore: NoteStore,
    settings: AppSettings,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val existingNote = remember(noteId) {
        if (noteId != null) noteStore.loadAll().firstOrNull { it.id == noteId } else null
    }

    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var body by remember { mutableStateOf(existingNote?.body ?: "") }
    var folder by remember { mutableStateOf(existingNote?.folder ?: "") }
    var selectedColorIdx by remember { mutableStateOf(existingNote?.colorIndex ?: settings.defaultNoteColor) }
    var attachments by remember { mutableStateOf(existingNote?.attachments ?: emptyList()) }
    var isChecklist by remember { mutableStateOf(existingNote?.isChecklist ?: false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val copied = noteStore.copyUriToInternalStorage(uri)
            if (copied != null) {
                attachments = attachments + copied
            }
        }
    }

    fun saveCurrentNote(): Long {
        val targetId = existingNote?.id ?: System.currentTimeMillis()
        val noteToSave = Note(
            id = targetId,
            title = title,
            body = body,
            isPinned = existingNote?.isPinned ?: false,
            isFavorite = existingNote?.isFavorite ?: false,
            folder = folder,
            isChecklist = isChecklist,
            colorIndex = selectedColorIdx,
            attachments = attachments,
            updatedAt = System.currentTimeMillis()
        )
        noteStore.save(noteToSave)
        return targetId
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "New Note" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (title.isNotBlank() || body.isNotBlank()) {
                            val id = saveCurrentNote()
                            onSaved(id)
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val id = saveCurrentNote()
                            onSaved(id)
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Done")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Formatting Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(onClick = { body += "**bold**" }) { Text("B", fontWeight = FontWeight.Bold) }
                FilledTonalButton(onClick = { body += "*italic*" }) { Text("I", fontWeight = FontWeight.SemiBold) }
                FilledTonalButton(onClick = { body += "# Heading\n" }) { Text("H1") }
                FilledTonalButton(onClick = { body += "\n- [ ] Task" }) { Text("☑️ Task") }
                FilledTonalButton(onClick = { body += "\n```\n// code\n```\n" }) { Text("Code") }
                FilledTonalButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) { Text("📎 Attach") }
            }

            // Accent Tint Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoteAccentColors.take(6).forEachIndexed { idx, color ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
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

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Folder
            OutlinedTextField(
                value = folder,
                onValueChange = { folder = it },
                label = { Text("Folder / Collection (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Body
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Note Content (Markdown supported)...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
            )

            // Attachments
            if (attachments.isNotEmpty()) {
                Text("Attachments (${attachments.size})", fontWeight = FontWeight.Bold)
                attachments.forEach { att ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (att.type == AttachmentType.IMAGE) "🖼️" else if (att.type == AttachmentType.VIDEO) "🎬" else "📄", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(att.displayName, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = { attachments = attachments.filter { it.id != att.id } }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    noteStore: NoteStore,
    canvasStore: CanvasStore,
    initialScope: String = "All",
    onOpenNote: (Long) -> Unit,
    onOpenCanvas: (String) -> Unit,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var scope by remember { mutableStateOf(initialScope) }

    val allNotes = remember { noteStore.loadAll() }
    val allCanvases = remember { canvasStore.loadAll() }

    val matchingNotes = remember(query, allNotes) {
        if (query.isBlank()) emptyList()
        else allNotes.filter {
            it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
        }
    }

    val matchingCanvases = remember(query, allCanvases) {
        if (query.isBlank()) emptyList()
        else allCanvases.filter {
            it.name.contains(query, ignoreCase = true) || it.nodes.any { n -> n.title.contains(query, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search workspace...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Scope filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Notes", "Canvases").forEach { s ->
                FilterChip(
                    selected = scope == s,
                    onClick = { scope = s },
                    label = { Text(s) }
                )
            }
        }

        HorizontalDivider()

        // Results
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (scope in listOf("All", "Notes")) {
                items(matchingNotes) { note ->
                    NoteCard(note = note, onClick = { onOpenNote(note.id) })
                }
            }

            if (scope in listOf("All", "Canvases")) {
                items(matchingCanvases) { doc ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCanvas(doc.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("🔀 ${doc.name}", fontWeight = FontWeight.Bold)
                            Text("${doc.nodes.size} nodes • ${doc.type.name.lowercase()}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

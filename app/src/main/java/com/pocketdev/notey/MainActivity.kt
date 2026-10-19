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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.io.File
import java.text.DateFormat
import java.util.Date

enum class NavDestination {
    HOME, NOTES, CANVAS, LIBRARY, SETTINGS
}

enum class ScreenState {
    SPLASH, ONBOARDING, PROFILE_SETUP, MAIN, NOTE_READER, NOTE_EDITOR, SEARCH
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
    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var activeViewerAttachment by remember { mutableStateOf<Attachment?>(null) }

    var screenState by remember {
        mutableStateOf(ScreenState.SPLASH)
    }
    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
    var activeNoteId by remember { mutableStateOf<Long?>(null) }
    var isStartingAsChecklist by remember { mutableStateOf(false) }
    var showQuickCreateSheet by remember { mutableStateOf(false) }
    var activeQuickEditNote by remember { mutableStateOf<Note?>(null) }
    var showCommandPalette by remember { mutableStateOf(false) }
    var canvasDocIdToOpen by remember { mutableStateOf<String?>(null) }
    var searchScopeFilter by remember { mutableStateOf("All") }

    val animDuration = if (!settings.animationsEnabled || settings.reduceMotion) 0 else settings.animationSpeed.durationMs

    // PATCH 1: Android System Back Button Bug Fix (Navigation Stack)
    BackHandler(enabled = true) {
        when {
            activeViewerAttachment != null -> {
                activeViewerAttachment = null
            }
            activeQuickEditNote != null -> {
                activeQuickEditNote = null
            }
            showQuickCreateSheet -> {
                showQuickCreateSheet = false
            }
            showCommandPalette -> {
                showCommandPalette = false
            }
            screenState == ScreenState.NOTE_READER || screenState == ScreenState.NOTE_EDITOR || screenState == ScreenState.SEARCH -> {
                screenState = ScreenState.MAIN
            }
            screenState == ScreenState.ONBOARDING || screenState == ScreenState.PROFILE_SETUP -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    (context as? ComponentActivity)?.finish()
                } else {
                    lastBackPressTime = now
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
            screenState == ScreenState.MAIN -> {
                if (currentDestination != NavDestination.HOME) {
                    currentDestination = NavDestination.HOME
                } else {
                    val now = System.currentTimeMillis()
                    if (now - lastBackPressTime < 2000L) {
                        (context as? ComponentActivity)?.finish()
                    } else {
                        lastBackPressTime = now
                        Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    }
                }
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
                    screenState = if (userProfileStore.hasProfile()) ScreenState.MAIN else ScreenState.ONBOARDING
                })
            }
            ScreenState.ONBOARDING -> {
                OnboardingScreen(onFinish = {
                    if (userProfileStore.hasProfile()) {
                        screenState = ScreenState.MAIN
                    } else {
                        screenState = ScreenState.PROFILE_SETUP
                    }
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
                    onEdit = { screenState = ScreenState.NOTE_EDITOR },
                    onPreviewMedia = { att -> activeViewerAttachment = att }
                )
            }
            ScreenState.NOTE_EDITOR -> {
                NoteEditorView(
                    noteId = activeNoteId,
                    noteStore = noteStore,
                    settings = settings,
                    initialIsChecklist = isStartingAsChecklist,
                    onBack = { screenState = ScreenState.MAIN },
                    onSaved = { savedId ->
                        activeNoteId = savedId
                        screenState = ScreenState.NOTE_READER
                    },
                    onPreviewMedia = { att -> activeViewerAttachment = att }
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
            ScreenState.MAIN -> {
                Scaffold(
                    bottomBar = {
                        // Bottom nav hidden when visual canvas is active
                        if (currentDestination != NavDestination.CANVAS) {
                            NavigationBar(
                                containerColor = NoteySurfaces.Surface1,
                                tonalElevation = 0.dp,
                                modifier = Modifier.border(width = 1.dp, color = NoteySurfaces.HairlineBorder)
                            ) {
                                NavigationBarItem(
                                    selected = currentDestination == NavDestination.HOME,
                                    onClick = { currentDestination = NavDestination.HOME },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                    label = { Text("Home") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF38BDF8),
                                        selectedTextColor = Color(0xFF38BDF8),
                                        indicatorColor = NoteySurfaces.Surface3,
                                        unselectedIconColor = NoteySurfaces.TextMuted,
                                        unselectedTextColor = NoteySurfaces.TextMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentDestination == NavDestination.NOTES,
                                    onClick = { currentDestination = NavDestination.NOTES },
                                    icon = { Icon(NoteyIcons.Document, contentDescription = "Notes") },
                                    label = { Text("Notes") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF38BDF8),
                                        selectedTextColor = Color(0xFF38BDF8),
                                        indicatorColor = NoteySurfaces.Surface3,
                                        unselectedIconColor = NoteySurfaces.TextMuted,
                                        unselectedTextColor = NoteySurfaces.TextMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentDestination == NavDestination.CANVAS,
                                    onClick = {
                                        canvasDocIdToOpen = null
                                        currentDestination = NavDestination.CANVAS
                                    },
                                    icon = { Icon(NoteyIcons.Workflow, contentDescription = "Canvas") },
                                    label = { Text("Canvas") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF38BDF8),
                                        selectedTextColor = Color(0xFF38BDF8),
                                        indicatorColor = NoteySurfaces.Surface3,
                                        unselectedIconColor = NoteySurfaces.TextMuted,
                                        unselectedTextColor = NoteySurfaces.TextMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentDestination == NavDestination.LIBRARY,
                                    onClick = { currentDestination = NavDestination.LIBRARY },
                                    icon = { Icon(NoteyIcons.Vault, contentDescription = "Vault") },
                                    label = { Text("Vault") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF38BDF8),
                                        selectedTextColor = Color(0xFF38BDF8),
                                        indicatorColor = NoteySurfaces.Surface3,
                                        unselectedIconColor = NoteySurfaces.TextMuted,
                                        unselectedTextColor = NoteySurfaces.TextMuted
                                    )
                                )
                                NavigationBarItem(
                                    selected = currentDestination == NavDestination.SETTINGS,
                                    onClick = { currentDestination = NavDestination.SETTINGS },
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                    label = { Text("Settings") },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF38BDF8),
                                        selectedTextColor = Color(0xFF38BDF8),
                                        indicatorColor = NoteySurfaces.Surface3,
                                        unselectedIconColor = NoteySurfaces.TextMuted,
                                        unselectedTextColor = NoteySurfaces.TextMuted
                                    )
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        if (currentDestination in listOf(NavDestination.HOME, NavDestination.NOTES)) {
                            FloatingActionButton(
                                onClick = { showQuickCreateSheet = true },
                                containerColor = Color(0xFF38BDF8),
                                contentColor = Color.Black,
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Create New")
                            }
                        }
                    },
                    containerColor = NoteySurfaces.Surface0
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
                                    onSearchClick = { showCommandPalette = true },
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
                                                isStartingAsChecklist = false
                                                screenState = ScreenState.NOTE_EDITOR
                                            }
                                            "checklist" -> {
                                                activeNoteId = null
                                                isStartingAsChecklist = true
                                                screenState = ScreenState.NOTE_EDITOR
                                            }
                                            "canvas" -> {
                                                canvasDocIdToOpen = null
                                                currentDestination = NavDestination.CANVAS
                                            }
                                            "vault" -> {
                                                currentDestination = NavDestination.LIBRARY
                                            }
                                        }
                                    },
                                    onQuickEdit = { note ->
                                        activeQuickEditNote = note
                                    }
                                )
                            }
                            NavDestination.NOTES -> {
                                NotesListView(
                                    noteStore = noteStore,
                                    userProfile = userProfile,
                                    onOpenNote = { id ->
                                        activeNoteId = id
                                        screenState = ScreenState.NOTE_READER
                                    },
                                    onCreateNote = {
                                        activeNoteId = null
                                        isStartingAsChecklist = false
                                        screenState = ScreenState.NOTE_EDITOR
                                    },
                                    onCreateChecklist = {
                                        activeNoteId = null
                                        isStartingAsChecklist = true
                                        screenState = ScreenState.NOTE_EDITOR
                                    },
                                    onSearchClick = { showCommandPalette = true },
                                    onOpenSettings = {
                                        currentDestination = NavDestination.SETTINGS
                                    },
                                    onQuickEdit = { note ->
                                        activeQuickEditNote = note
                                    }
                                )
                            }
                            NavDestination.CANVAS -> {
                                CanvasScreen(
                                    canvasStore = canvasStore,
                                    settings = settings,
                                    noteStore = noteStore,
                                    initialDocId = canvasDocIdToOpen,
                                    onBack = { currentDestination = NavDestination.HOME },
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

    // Command Palette Modal Launcher
    if (showCommandPalette) {
        CommandPaletteDialog(
            noteStore = noteStore,
            canvasStore = canvasStore,
            onDismiss = { showCommandPalette = false },
            onOpenNote = { id ->
                showCommandPalette = false
                activeNoteId = id
                screenState = ScreenState.NOTE_READER
            },
            onOpenCanvas = { cid ->
                showCommandPalette = false
                canvasDocIdToOpen = cid
                currentDestination = NavDestination.CANVAS
                screenState = ScreenState.MAIN
            },
            onAction = { action ->
                showCommandPalette = false
                when (action) {
                    "note" -> {
                        activeNoteId = null
                        isStartingAsChecklist = false
                        screenState = ScreenState.NOTE_EDITOR
                    }
                    "checklist" -> {
                        activeNoteId = null
                        isStartingAsChecklist = true
                        screenState = ScreenState.NOTE_EDITOR
                    }
                    "canvas" -> {
                        canvasDocIdToOpen = null
                        currentDestination = NavDestination.CANVAS
                        screenState = ScreenState.MAIN
                    }
                    "vault" -> {
                        currentDestination = NavDestination.LIBRARY
                        screenState = ScreenState.MAIN
                    }
                    "settings" -> {
                        currentDestination = NavDestination.SETTINGS
                        screenState = ScreenState.MAIN
                    }
                }
            }
        )
    }

    // Card Quick Edit & File Manager Bottom Sheet
    if (activeQuickEditNote != null) {
        CardQuickEditBottomSheet(
            note = activeQuickEditNote!!,
            noteStore = noteStore,
            onDismiss = { activeQuickEditNote = null },
            onSaved = { activeQuickEditNote = null }
        )
    }

    // Full-Screen Hardware-Accelerated Media Viewer Dialog
    if (activeViewerAttachment != null) {
        val att = activeViewerAttachment!!
        MediaViewerDialog(
            filePath = att.filePath,
            title = att.displayName,
            onDismiss = { activeViewerAttachment = null }
        )
    }

    // Quick Create Modal Bottom Sheet
    if (showQuickCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickCreateSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = NoteySurfaces.Surface2
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create New",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NoteySurfaces.TextPrimary
                    )
                    IconButton(onClick = { showQuickCreateSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NoteySurfaces.TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                QuickCreateMenuItem(
                    icon = NoteyIcons.Document,
                    title = "New Document",
                    subtitle = "Capture ideas in deep focus markdown",
                    onClick = {
                        showQuickCreateSheet = false
                        activeNoteId = null
                        isStartingAsChecklist = false
                        screenState = ScreenState.NOTE_EDITOR
                    }
                )

                QuickCreateMenuItem(
                    icon = NoteyIcons.TaskCheck,
                    title = "New Checklist",
                    subtitle = "Structured action items and todos",
                    onClick = {
                        showQuickCreateSheet = false
                        activeNoteId = null
                        isStartingAsChecklist = true
                        screenState = ScreenState.NOTE_EDITOR
                    }
                )

                QuickCreateMenuItem(
                    icon = NoteyIcons.Workflow,
                    title = "New Visual Canvas",
                    subtitle = "Infinite board with nodes & connections",
                    onClick = {
                        showQuickCreateSheet = false
                        canvasDocIdToOpen = null
                        currentDestination = NavDestination.CANVAS
                    }
                )

                QuickCreateMenuItem(
                    icon = NoteyIcons.Vault,
                    title = "Asset Vault",
                    subtitle = "Browse library of files and media",
                    onClick = {
                        showQuickCreateSheet = false
                        currentDestination = NavDestination.LIBRARY
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
        shape = RoundedCornerShape(12.dp),
        color = NoteySurfaces.Surface1,
        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NoteySurfaces.Surface3),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = NoteySurfaces.TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = NoteySurfaces.TextSecondary)
            }
        }
    }
}


@Composable
fun CreationMatrixItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = NoteySurfaces.Surface1,
        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = NoteySurfaces.TextPrimary
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
    onQuickAction: (String) -> Unit,
    onQuickEdit: (Note) -> Unit
) {
    val activeNotes = remember { noteStore.getActiveNotes() }
    val canvases = remember { canvasStore.loadAll() }

    var selectedWorkspace by remember { mutableStateOf("Personal Workspace") }
    var showWorkspaceMenu by remember { mutableStateOf(false) }
    val workspaces = listOf("Personal Workspace", "Engineering", "Design", "Research")

    var homeFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Pinned", "Documents", "Workflows")

    val greetingText = remember(userProfile?.displayName) {
        UserProfileValidator.getTimeAwareGreeting(userProfile?.displayName ?: "")
    }

    val displayedNotes = remember(activeNotes, homeFilter) {
        when (homeFilter) {
            "Pinned" -> activeNotes.filter { it.isPinned }
            "Documents" -> activeNotes.filter { !it.isChecklist }
            "Workflows" -> emptyList()
            else -> activeNotes
        }
    }

    val pinnedNotes = remember(displayedNotes) { displayedNotes.filter { it.isPinned } }
    val recentNotes = remember(displayedNotes) { displayedNotes.filter { !it.isPinned } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Workspace Switcher & Profile Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Workspace Dropdown Capsule
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NoteySurfaces.Surface2,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
                    modifier = Modifier.clickable { showWorkspaceMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8))
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = selectedWorkspace,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = NoteySurfaces.TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = NoteySurfaces.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showWorkspaceMenu,
                    onDismissRequest = { showWorkspaceMenu = false },
                    modifier = Modifier.background(NoteySurfaces.Surface2)
                ) {
                    workspaces.forEach { ws ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (ws == selectedWorkspace) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                    } else {
                                        Spacer(modifier = Modifier.width(24.dp))
                                    }
                                    Text(ws, color = NoteySurfaces.TextPrimary)
                                }
                            },
                            onClick = {
                                selectedWorkspace = ws
                                showWorkspaceMenu = false
                            }
                        )
                    }
                }
            }

            // Initials Avatar
            val avatarBitmap = remember(userProfile?.avatarUri) {
                if (userProfile?.avatarUri != null) {
                    try { BitmapFactory.decodeFile(userProfile.avatarUri) } catch (_: Exception) { null }
                } else null
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(NoteySurfaces.Surface2)
                    .border(1.dp, NoteySurfaces.HairlineBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = UserProfileValidator.getInitials(userProfile?.displayName ?: "User"),
                        style = MonoMetadataStyle,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        }

        // Greeting
        Column {
            Text(
                text = greetingText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = NoteySurfaces.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Workspace is synced and local-first.",
                style = MonoMetadataStyle
            )
        }

        // Omni-Search Capsule with ⌘K Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NoteySurfaces.Surface2,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSearchClick)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = NoteySurfaces.TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Search workspace, notes, commands...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NoteySurfaces.TextMuted,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = NoteySurfaces.Surface4,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder)
                ) {
                    Text(
                        text = "⌘K",
                        style = MonoMetadataStyle,
                        color = NoteySurfaces.TextSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 4-Item Creation Matrix
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CreationMatrixItem(
                icon = NoteyIcons.Document,
                label = "Doc",
                accent = Color(0xFF38BDF8),
                onClick = { onQuickAction("note") },
                modifier = Modifier.weight(1f)
            )
            CreationMatrixItem(
                icon = NoteyIcons.TaskCheck,
                label = "Task",
                accent = Color(0xFF34D399),
                onClick = { onQuickAction("checklist") },
                modifier = Modifier.weight(1f)
            )
            CreationMatrixItem(
                icon = NoteyIcons.Workflow,
                label = "Flow",
                accent = Color(0xFFA78BFA),
                onClick = { onQuickAction("canvas") },
                modifier = Modifier.weight(1f)
            )
            CreationMatrixItem(
                icon = NoteyIcons.Vault,
                label = "Vault",
                accent = Color(0xFFFBBF24),
                onClick = { onQuickAction("vault") },
                modifier = Modifier.weight(1f)
            )
        }

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filterOptions.forEach { filter ->
                val isSelected = homeFilter == filter
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) NoteySurfaces.Surface3 else NoteySurfaces.Surface1,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF38BDF8) else NoteySurfaces.HairlineBorder
                    ),
                    modifier = Modifier.clickable { homeFilter = filter }
                ) {
                    Text(
                        text = filter,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF38BDF8) else NoteySurfaces.TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Pinned Section
        if (pinnedNotes.isNotEmpty() && homeFilter != "Workflows") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        NoteyIcons.Pin,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF38BDF8)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PINNED",
                        style = MonoMetadataStyle,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }

                pinnedNotes.forEach { note ->
                    NoteCard(
                        note = note,
                        onClick = { onOpenNote(note.id) },
                        onTogglePin = { noteStore.togglePin(note.id) },
                        onQuickEdit = { onQuickEdit(note) }
                    )
                }
            }
        }

        // Recent Notes Section
        if (homeFilter != "Workflows") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "DOCUMENTS & TASKS",
                    style = MonoMetadataStyle,
                    fontWeight = FontWeight.Bold
                )

                if (recentNotes.isEmpty() && pinnedNotes.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NoteySurfaces.Surface1,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(NoteyIcons.Document, contentDescription = null, tint = NoteySurfaces.TextMuted, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No documents yet", fontWeight = FontWeight.SemiBold, color = NoteySurfaces.TextPrimary)
                            Text("Tap Doc or Task to create your first item.", style = MonoMetadataStyle)
                        }
                    }
                } else {
                    recentNotes.forEach { note ->
                        NoteCard(
                            note = note,
                            onClick = { onOpenNote(note.id) },
                            onTogglePin = { noteStore.togglePin(note.id) },
                            onQuickEdit = { onQuickEdit(note) }
                        )
                    }
                }
            }
        }

        // Canvases & Workflows
        if ((homeFilter == "All" || homeFilter == "Workflows") && canvases.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "VISUAL WORKFLOWS",
                    style = MonoMetadataStyle,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(canvases) { doc ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NoteySurfaces.Surface1,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
                            modifier = Modifier
                                .width(220.dp)
                                .clickable { onOpenCanvas(doc.id) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        NoteyIcons.Workflow,
                                        contentDescription = null,
                                        tint = Color(0xFFA78BFA),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = doc.type.name,
                                        style = MonoMetadataStyle,
                                        color = Color(0xFFA78BFA),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = doc.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = NoteySurfaces.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${doc.nodes.size} nodes • ${doc.connectors.size} connections",
                                    style = MonoMetadataStyle
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
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
        shape = RoundedCornerShape(10.dp),
        color = NoteySurfaces.Surface1,
        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = NoteySurfaces.TextPrimary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListView(
    noteStore: NoteStore,
    userProfile: UserProfile?,
    onOpenNote: (Long) -> Unit,
    onCreateNote: () -> Unit,
    onCreateChecklist: () -> Unit,
    onSearchClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuickEdit: (Note) -> Unit
) {
    var notes by remember { mutableStateOf(noteStore.getActiveNotes()) }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedFolder by remember { mutableStateOf("") }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(true) }

    val folders = remember(notes) { noteStore.getFolders() }

    val filteredNotes = remember(notes, selectedFilter, selectedFolder) {
        notes.filter { note ->
            val matchesFolder = selectedFolder.isBlank() || note.folder.equals(selectedFolder, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                "Notes" -> !note.isChecklist
                "Tasks" -> note.isChecklist || note.body.contains("- [")
                "Pinned" -> note.isPinned
                "Favorites" -> note.isFavorite
                else -> true
            }
            matchesFolder && matchesFilter
        }
    }

    val pinnedNotes = remember(filteredNotes) { filteredNotes.filter { it.isPinned } }
    val otherNotes = remember(filteredNotes) { filteredNotes.filter { !it.isPinned } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoteySurfaces.Surface0)
    ) {
        // Top Search Bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NoteySurfaces.Surface2,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = NoteySurfaces.TextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onSearchClick)
                        .padding(vertical = 10.dp)
                ) {
                    Text(
                        text = "Search workspace...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NoteySurfaces.TextMuted
                    )
                }
                IconButton(onClick = { isGridView = !isGridView }) {
                    Icon(
                        imageVector = if (isGridView) NoteyIcons.ViewAgenda else NoteyIcons.GridView,
                        contentDescription = "Toggle View",
                        tint = NoteySurfaces.TextSecondary
                    )
                }

                // Profile Avatar Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NoteySurfaces.Surface3)
                        .border(1.dp, NoteySurfaces.HairlineBorder, CircleShape)
                        .clickable { showAccountDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    val avatarBmp = remember(userProfile?.avatarUri) {
                        if (userProfile?.avatarUri != null) {
                            try { BitmapFactory.decodeFile(userProfile.avatarUri) } catch (_: Exception) { null }
                        } else null
                    }
                    if (avatarBmp != null) {
                        Image(
                            bitmap = avatarBmp.asImageBitmap(),
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = UserProfileValidator.getInitials(userProfile?.displayName ?: "U"),
                            style = MonoMetadataStyle,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("All", "Notes", "Tasks", "Pinned", "Favorites").forEach { filter ->
                val isSelected = selectedFilter == filter && selectedFolder.isBlank()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) NoteySurfaces.Surface3 else NoteySurfaces.Surface1,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF38BDF8) else NoteySurfaces.HairlineBorder
                    ),
                    modifier = Modifier.clickable {
                        selectedFilter = filter
                        selectedFolder = ""
                    }
                ) {
                    Text(
                        text = filter,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF38BDF8) else NoteySurfaces.TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (folders.isNotEmpty()) {
                folders.forEach { fName ->
                    val isSelected = selectedFolder == fName
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) NoteySurfaces.Surface3 else NoteySurfaces.Surface1,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF38BDF8) else NoteySurfaces.HairlineBorder
                        ),
                        modifier = Modifier.clickable {
                            selectedFilter = "Folder"
                            selectedFolder = fName
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(NoteyIcons.Folder, contentDescription = null, tint = NoteySurfaces.TextSecondary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = fName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color(0xFF38BDF8) else NoteySurfaces.TextSecondary
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = NoteySurfaces.Surface1,
                border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
                modifier = Modifier.clickable { showFolderDialog = true }
            ) {
                Text(
                    text = "+ Folder",
                    style = MaterialTheme.typography.labelSmall,
                    color = NoteySurfaces.TextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        // Notes Display (Empty state vs Grid vs List)
        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(NoteyIcons.Document, contentDescription = null, tint = NoteySurfaces.TextMuted, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedFilter == "Tasks") "No tasks found" else "No notes found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NoteySurfaces.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Notes you add appear here.",
                        style = MonoMetadataStyle
                    )
                }
            }
        } else if (isGridView) {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (pinnedNotes.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            text = "PINNED",
                            style = MonoMetadataStyle,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 2.dp)
                        )
                    }
                    items(pinnedNotes, key = { it.id }) { note ->
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
                            },
                            onToggleTask = { taskIdx ->
                                noteStore.toggleTask(note.id, taskIdx)
                                notes = noteStore.getActiveNotes()
                            },
                            onQuickEdit = { onQuickEdit(note) }
                        )
                    }
                    if (otherNotes.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                text = "OTHERS",
                                style = MonoMetadataStyle,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 2.dp)
                            )
                        }
                    }
                }

                items(otherNotes, key = { it.id }) { note ->
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
                        },
                        onToggleTask = { taskIdx ->
                            noteStore.toggleTask(note.id, taskIdx)
                            notes = noteStore.getActiveNotes()
                        },
                        onQuickEdit = { onQuickEdit(note) }
                    )
                }

                item(span = StaggeredGridItemSpan.FullLine) {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pinnedNotes.isNotEmpty()) {
                    item {
                        Text(
                            text = "PINNED",
                            style = MonoMetadataStyle,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(start = 6.dp, top = 6.dp, bottom = 2.dp)
                        )
                    }
                    items(pinnedNotes, key = { it.id }) { note ->
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
                            },
                            onToggleTask = { taskIdx ->
                                noteStore.toggleTask(note.id, taskIdx)
                                notes = noteStore.getActiveNotes()
                            },
                            onQuickEdit = { onQuickEdit(note) }
                        )
                    }
                    if (otherNotes.isNotEmpty()) {
                        item {
                            Text(
                                text = "OTHERS",
                                style = MonoMetadataStyle,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 6.dp, top = 14.dp, bottom = 2.dp)
                            )
                        }
                    }
                }

                items(otherNotes, key = { it.id }) { note ->
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
                        },
                        onToggleTask = { taskIdx ->
                            noteStore.toggleTask(note.id, taskIdx)
                            notes = noteStore.getActiveNotes()
                        },
                        onQuickEdit = { onQuickEdit(note) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            containerColor = NoteySurfaces.Surface2,
            title = {
                Text("Notey Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NoteySurfaces.TextPrimary)
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val avatarBmp = remember(userProfile?.avatarUri) {
                        if (userProfile?.avatarUri != null) {
                            try { BitmapFactory.decodeFile(userProfile.avatarUri) } catch (_: Exception) { null }
                        } else null
                    }
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(NoteySurfaces.Surface3)
                            .border(1.dp, NoteySurfaces.HairlineBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBmp != null) {
                            Image(
                                bitmap = avatarBmp.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = UserProfileValidator.getInitials(userProfile?.displayName ?: "User"),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = userProfile?.displayName ?: "Notey User",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NoteySurfaces.TextPrimary
                    )
                    Text(
                        text = "Built by Yathin",
                        style = MonoMetadataStyle
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            showAccountDialog = false
                            onOpenSettings()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Manage Account & Preferences")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showFolderDialog) {
        var folderNameInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            containerColor = NoteySurfaces.Surface2,
            title = { Text("Filter by Folder", color = NoteySurfaces.TextPrimary) },
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
                    Text("Filter")
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
    onToggleFavorite: (() -> Unit)? = null,
    onToggleTask: ((Int) -> Unit)? = null,
    onQuickEdit: ((Note) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val accentColor = GoogleKeepPalette.getAccentColor(note.colorIndex)
    val cardBg = if (isDark) NoteySurfaces.Surface1 else Color.White
    val textColor = if (isDark) NoteySurfaces.TextPrimary else Color(0xFF1E293B)
    val subTextColor = if (isDark) NoteySurfaces.TextSecondary else Color(0xFF64748B)
    val dateStr = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(note.updatedAt))

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        tonalElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // 3.5dp Dynamic Accent Left Border
                drawRect(
                    color = accentColor,
                    topLeft = Offset.Zero,
                    size = Size(3.5.dp.toPx(), size.height)
                )
            }
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp)
        ) {
            // Header Row: Title, Quick Edit Pencil, and Pin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onQuickEdit != null) {
                        IconButton(
                            onClick = { onQuickEdit(note) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                NoteyIcons.Pencil,
                                contentDescription = "Quick edit",
                                tint = subTextColor,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    if (onTogglePin != null) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                if (note.isPinned) NoteyIcons.Pin else NoteyIcons.PinOff,
                                contentDescription = if (note.isPinned) "Unpin" else "Pin",
                                tint = if (note.isPinned) accentColor else subTextColor.copy(alpha = 0.5f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            // Body or Tasks
            val tasks = remember(note.body) { note.parseTasks() }
            if (note.isChecklist || (tasks.isNotEmpty() && note.body.contains("- ["))) {
                Spacer(modifier = Modifier.height(6.dp))
                val visibleTasks = tasks.take(4)
                visibleTasks.forEachIndexed { idx, task ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = task.isDone,
                            onCheckedChange = { onToggleTask?.invoke(idx) },
                            modifier = Modifier.size(18.dp),
                            colors = CheckboxDefaults.colors(
                                checkedColor = accentColor,
                                uncheckedColor = subTextColor.copy(alpha = 0.5f),
                                checkmarkColor = getInvertedTextColor(accentColor)
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = task.text,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (task.isDone) subTextColor.copy(alpha = 0.5f) else textColor,
                            textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (tasks.size > 4) {
                    Text(
                        text = "+ ${tasks.size - 4} more items",
                        style = MonoMetadataStyle,
                        modifier = Modifier.padding(start = 24.dp, top = 2.dp)
                    )
                }
            } else if (note.body.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subTextColor,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Image Preview if any image attachment
            val firstImage = note.attachments.firstOrNull { it.type == AttachmentType.IMAGE }
            if (firstImage != null) {
                val bmp = remember(firstImage.filePath) {
                    try { BitmapFactory.decodeFile(firstImage.filePath) } catch (_: Exception) { null }
                }
                if (bmp != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Inline File Chips with Paperclip Vector Icon
            if (note.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    note.attachments.take(3).forEach { att ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = NoteySurfaces.Surface3,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    NoteyIcons.Paperclip,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = att.displayName,
                                    style = MonoMetadataStyle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 110.dp)
                                )
                            }
                        }
                    }
                    if (note.attachments.size > 3) {
                        Text(
                            text = "+${note.attachments.size - 3}",
                            style = MonoMetadataStyle
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Row: Inverted Accent Pill for Folder/Tags, Date in Mono, and Favorite Star
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (note.folder.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = accentColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    NoteyIcons.Folder,
                                    contentDescription = null,
                                    tint = getInvertedTextColor(accentColor),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = note.folder,
                                    style = MonoMetadataStyle,
                                    fontWeight = FontWeight.Bold,
                                    color = getInvertedTextColor(accentColor)
                                )
                            }
                        }
                    } else if (note.tags.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NoteySurfaces.Surface4
                        ) {
                            Text(
                                text = "#${note.tags.first()}",
                                style = MonoMetadataStyle,
                                color = NoteySurfaces.TextSecondary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = dateStr,
                            style = MonoMetadataStyle
                        )
                    }
                }

                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = if (note.isFavorite) Color(0xFFF59E0B) else subTextColor.copy(alpha = 0.35f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun CommandPaletteDialog(
    noteStore: NoteStore,
    canvasStore: CanvasStore,
    onDismiss: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenCanvas: (String) -> Unit,
    onAction: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val allNotes = remember { noteStore.loadAll() }
    val allCanvases = remember { canvasStore.loadAll() }

    val matchedNotes = remember(query, allNotes) {
        if (query.isBlank()) emptyList()
        else allNotes.filter {
            it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
        }.take(5)
    }

    val matchedCanvases = remember(query, allCanvases) {
        if (query.isBlank()) emptyList()
        else allCanvases.filter {
            it.name.contains(query, ignoreCase = true) || it.nodes.any { n -> n.title.contains(query, ignoreCase = true) }
        }.take(5)
    }

    val quickActions = remember(query) {
        val actions = listOf(
            Triple("New Note", "Create a blank markdown document", "note"),
            Triple("New Checklist", "Create a structured task checklist", "checklist"),
            Triple("New Workflow Canvas", "Open visual infinite workspace", "canvas"),
            Triple("Open Asset Vault", "Browse files, documents & media", "vault"),
            Triple("Preferences", "Adjust theme and animations", "settings")
        )
        if (query.isBlank()) actions
        else actions.filter { it.first.contains(query, ignoreCase = true) || it.second.contains(query, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NoteySurfaces.Surface2,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.72f)
                .imePadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Command palette search input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = NoteySurfaces.TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        textStyle = TextStyle(color = NoteySurfaces.TextPrimary, fontSize = 16.sp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF38BDF8)),
                        decorationBox = { inner ->
                            if (query.isBlank()) {
                                Text("Type a command, note or workflow...", color = NoteySurfaces.TextMuted, fontSize = 15.sp)
                            }
                            inner()
                        }
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = NoteySurfaces.Surface4,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder)
                    ) {
                        Text(
                            text = "⌘K",
                            style = MonoMetadataStyle,
                            color = NoteySurfaces.TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                HorizontalDivider(color = NoteySurfaces.HairlineBorder)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (matchedNotes.isNotEmpty()) {
                        item {
                            Text(
                                text = "NOTES",
                                style = MonoMetadataStyle,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        items(matchedNotes) { note ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenNote(note.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(NoteyIcons.Document, contentDescription = null, tint = GoogleKeepPalette.getAccentColor(note.colorIndex), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (note.title.isNotBlank()) note.title else "Untitled Note", style = MaterialTheme.typography.bodyMedium, color = NoteySurfaces.TextPrimary, maxLines = 1)
                                        Text(note.body.take(60), style = MonoMetadataStyle, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    if (matchedCanvases.isNotEmpty()) {
                        item {
                            Text(
                                text = "WORKFLOWS & CANVASES",
                                style = MonoMetadataStyle,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        items(matchedCanvases) { canvas ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenCanvas(canvas.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(NoteyIcons.Workflow, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(canvas.name, style = MaterialTheme.typography.bodyMedium, color = NoteySurfaces.TextPrimary, modifier = Modifier.weight(1f))
                                    Text("${canvas.nodes.size} nodes", style = MonoMetadataStyle)
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "COMMANDS & ACTIONS",
                            style = MonoMetadataStyle,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    items(quickActions) { (actionTitle, subtitle, actionKey) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAction(actionKey) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val icon = when (actionKey) {
                                    "note" -> NoteyIcons.Document
                                    "checklist" -> NoteyIcons.TaskCheck
                                    "canvas" -> NoteyIcons.Workflow
                                    "vault" -> NoteyIcons.Vault
                                    else -> Icons.Default.Settings
                                }
                                Icon(icon, contentDescription = null, tint = NoteySurfaces.TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(actionTitle, style = MaterialTheme.typography.bodyMedium, color = NoteySurfaces.TextPrimary)
                                    Text(subtitle, style = MonoMetadataStyle)
                                }
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
fun NoteReaderView(
    noteId: Long,
    noteStore: NoteStore,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPreviewMedia: ((Attachment) -> Unit)? = null
) {
    val context = LocalContext.current
    var note by remember(noteId) { mutableStateOf(noteStore.loadAll().firstOrNull { it.id == noteId }) }
    var showExportSheet by remember { mutableStateOf(false) }

    if (note == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val currentNote = note!!
    val accentColor = GoogleKeepPalette.getAccentColor(currentNote.colorIndex)
    val wordCount = remember(currentNote.body) {
        currentNote.body.split(Regex("\\s+")).count { it.isNotBlank() }
    }
    val charCount = currentNote.body.length
    val readTime = (wordCount / 200).coerceAtLeast(1)
    val updatedDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(currentNote.updatedAt))

    Scaffold(
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.2f))))
                )
                TopAppBar(
                    title = {
                        Text(
                            text = if (currentNote.isChecklist) "Task Checklist" else "Document",
                            style = MaterialTheme.typography.titleMedium,
                            color = NoteySurfaces.TextPrimary
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NoteySurfaces.Surface0,
                        navigationIconContentColor = NoteySurfaces.TextPrimary,
                        actionIconContentColor = NoteySurfaces.TextPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showExportSheet = true }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share & Export")
                        }
                        IconButton(
                            onClick = {
                                noteStore.togglePin(currentNote.id)
                                note = currentNote.copy(isPinned = !currentNote.isPinned)
                            }
                        ) {
                            Icon(
                                if (currentNote.isPinned) NoteyIcons.Pin else NoteyIcons.PinOff,
                                contentDescription = "Pin",
                                tint = if (currentNote.isPinned) accentColor else NoteySurfaces.TextSecondary
                            )
                        }
                        IconButton(
                            onClick = {
                                noteStore.toggleFavorite(currentNote.id)
                                note = currentNote.copy(isFavorite = !currentNote.isFavorite)
                            }
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = if (currentNote.isFavorite) Color(0xFFF59E0B) else NoteySurfaces.TextMuted.copy(alpha = 0.35f)
                            )
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
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onEdit,
                containerColor = accentColor,
                contentColor = getInvertedTextColor(accentColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(NoteyIcons.Pencil, contentDescription = "Edit Note")
            }
        },
        containerColor = NoteySurfaces.Surface0
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
            if (currentNote.title.isNotBlank()) {
                Text(
                    text = currentNote.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NoteySurfaces.TextPrimary
                )
            }

            // Monospace Metadata Ribbon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$wordCount words • $charCount chars • $readTime min read",
                    style = MonoMetadataStyle
                )
                if (currentNote.folder.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(NoteyIcons.Folder, contentDescription = null, tint = getInvertedTextColor(accentColor), modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentNote.folder,
                                style = MonoMetadataStyle,
                                fontWeight = FontWeight.Bold,
                                color = getInvertedTextColor(accentColor)
                            )
                        }
                    }
                }
            }

            Text(
                text = "Updated $updatedDate",
                style = MonoMetadataStyle
            )

            HorizontalDivider(color = NoteySurfaces.HairlineBorder)

            // Tasks or Note Body
            val tasks = remember(currentNote.body) { currentNote.parseTasks() }
            if (currentNote.isChecklist || (tasks.isNotEmpty() && currentNote.body.contains("- ["))) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tasks.forEachIndexed { idx, task ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    noteStore.toggleTask(currentNote.id, idx)
                                    note = noteStore.loadAll().firstOrNull { it.id == currentNote.id }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = task.isDone,
                                onCheckedChange = {
                                    noteStore.toggleTask(currentNote.id, idx)
                                    note = noteStore.loadAll().firstOrNull { it.id == currentNote.id }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentColor,
                                    uncheckedColor = NoteySurfaces.TextSecondary.copy(alpha = 0.5f),
                                    checkmarkColor = getInvertedTextColor(accentColor)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = task.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (task.isDone) NoteySurfaces.TextMuted else NoteySurfaces.TextPrimary,
                                textDecoration = if (task.isDone) TextDecoration.LineThrough else null
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = currentNote.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = NoteySurfaces.TextPrimary,
                    lineHeight = 24.sp
                )
            }

            // Media Attachments
            if (currentNote.attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "ATTACHMENTS (${currentNote.attachments.size})",
                    style = MonoMetadataStyle,
                    fontWeight = FontWeight.Bold
                )

                currentNote.attachments.forEach { att ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NoteySurfaces.Surface1,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onPreviewMedia?.invoke(att) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (att.type == AttachmentType.IMAGE) {
                                val bmp = remember(att.filePath) {
                                    try { BitmapFactory.decodeFile(att.filePath) } catch (_: Exception) { null }
                                }
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(NoteyIcons.Paperclip, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(att.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = NoteySurfaces.TextPrimary)
                                    Text("${(att.sizeBytes / 1024).coerceAtLeast(1)} KB", style = MonoMetadataStyle)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    if (showExportSheet) {
        ExportShareBottomSheet(
            note = currentNote,
            onDismiss = { showExportSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorView(
    noteId: Long?,
    noteStore: NoteStore,
    settings: AppSettings,
    initialIsChecklist: Boolean = false,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onPreviewMedia: ((Attachment) -> Unit)? = null
) {
    val existingNote = remember(noteId) {
        if (noteId != null) noteStore.loadAll().firstOrNull { it.id == noteId } else null
    }

    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var folder by remember { mutableStateOf(existingNote?.folder ?: "") }
    var selectedColorIdx by remember { mutableStateOf(existingNote?.colorIndex ?: settings.defaultNoteColor) }
    var selectedBgTheme by remember { mutableStateOf(existingNote?.bgTheme ?: "none") }
    var attachments by remember { mutableStateOf(existingNote?.attachments ?: emptyList()) }
    var isChecklist by remember { mutableStateOf(existingNote?.isChecklist ?: initialIsChecklist) }
    var isPinned by remember { mutableStateOf(existingNote?.isPinned ?: false) }
    var isFavorite by remember { mutableStateOf(existingNote?.isFavorite ?: false) }

    var showThemeSheet by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showCompletedTasks by remember { mutableStateOf(true) }
    var showExportSheet by remember { mutableStateOf(false) }

    var tasks by remember {
        val initial = existingNote?.parseTasks() ?: emptyList()
        mutableStateOf(
            if (initial.isNotEmpty()) initial
            else if (initialIsChecklist) listOf(TaskItem(text = "", isDone = false))
            else emptyList()
        )
    }

    var bodyTextFieldValue by remember {
        mutableStateOf(TextFieldValue(existingNote?.body ?: ""))
    }

    val accentColor = GoogleKeepPalette.getAccentColor(selectedColorIdx)

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

    fun insertMarkdown(prefix: String, suffix: String = "") {
        if (isChecklist) {
            isChecklist = false
            bodyTextFieldValue = TextFieldValue(tasks.joinToString("\n") { (if (it.isDone) "- [x] " else "- [ ] ") + it.text })
        }
        val currentText = bodyTextFieldValue.text
        val selection = bodyTextFieldValue.selection
        val start = selection.min.coerceIn(0, currentText.length)
        val end = selection.max.coerceIn(0, currentText.length)
        val selectedText = currentText.substring(start, end)
        val newText = currentText.substring(0, start) + prefix + selectedText + suffix + currentText.substring(end)
        val newCursor = if (selectedText.isEmpty()) {
            start + prefix.length
        } else {
            start + prefix.length + selectedText.length + suffix.length
        }
        bodyTextFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    fun toggleChecklistMode() {
        if (isChecklist) {
            bodyTextFieldValue = TextFieldValue(tasks.joinToString("\n") { it.text })
            isChecklist = false
        } else {
            val parsed = bodyTextFieldValue.text.lines().mapNotNull { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) null
                else if (trimmed.startsWith("- [x] ", ignoreCase = true)) {
                    TaskItem(text = trimmed.removePrefix("- [x] ").removePrefix("- [X] "), isDone = true)
                } else if (trimmed.startsWith("- [ ] ")) {
                    TaskItem(text = trimmed.removePrefix("- [ ] "), isDone = false)
                } else {
                    TaskItem(text = trimmed, isDone = false)
                }
            }
            tasks = if (parsed.isNotEmpty()) parsed else listOf(TaskItem(text = "", isDone = false))
            isChecklist = true
        }
    }

    fun saveCurrentNote(): Long {
        val finalBody = if (isChecklist) {
            tasks.filter { it.text.isNotBlank() }.joinToString("\n") { task ->
                val prefix = if (task.isDone) "- [x] " else "- [ ] "
                "$prefix${task.text}"
            }
        } else {
            bodyTextFieldValue.text
        }
        val targetId = existingNote?.id ?: System.currentTimeMillis()
        val noteToSave = Note(
            id = targetId,
            title = title,
            body = finalBody,
            isPinned = isPinned,
            isFavorite = isFavorite,
            folder = folder,
            isChecklist = isChecklist,
            colorIndex = selectedColorIdx,
            bgTheme = selectedBgTheme,
            attachments = attachments,
            updatedAt = System.currentTimeMillis()
        )
        noteStore.save(noteToSave)
        return targetId
    }

    Scaffold(
        topBar = {
            Column {
                // 2px Luxury Gradient Accent Top Edge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.2f))))
                )
                TopAppBar(
                    title = { },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NoteySurfaces.Surface0,
                        navigationIconContentColor = NoteySurfaces.TextPrimary,
                        actionIconContentColor = NoteySurfaces.TextPrimary
                    ),
                    navigationIcon = {
                        IconButton(onClick = {
                            val hasContent = title.isNotBlank() || (if (isChecklist) tasks.any { it.text.isNotBlank() } else bodyTextFieldValue.text.isNotBlank())
                            if (hasContent) {
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
                        IconButton(onClick = { isPinned = !isPinned }) {
                            Icon(
                                if (isPinned) NoteyIcons.Pin else NoteyIcons.PinOff,
                                contentDescription = "Pin",
                                tint = if (isPinned) accentColor else NoteySurfaces.TextSecondary
                            )
                        }
                        IconButton(onClick = { isFavorite = !isFavorite }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFF59E0B) else NoteySurfaces.TextMuted.copy(alpha = 0.35f)
                            )
                        }
                        IconButton(onClick = { showFolderDialog = true }) {
                            Icon(NoteyIcons.Folder, contentDescription = "Folder", tint = NoteySurfaces.TextSecondary)
                        }
                        IconButton(
                            onClick = {
                                saveCurrentNote()
                                showExportSheet = true
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share & Export", tint = NoteySurfaces.TextPrimary)
                        }
                        IconButton(
                            onClick = {
                                val id = saveCurrentNote()
                                onSaved(id)
                            }
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Save", tint = accentColor)
                        }
                    }
                )
            }
        },
        bottomBar = {
            // Floating Markdown Accessory Dock pinned right above the keyboard
            Surface(
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                color = NoteySurfaces.Surface2,
                border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { insertMarkdown("**", "**") }, modifier = Modifier.size(34.dp)) {
                            Icon(NoteyIcons.Bold, contentDescription = "Bold", tint = NoteySurfaces.TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { insertMarkdown("*", "*") }, modifier = Modifier.size(34.dp)) {
                            Icon(NoteyIcons.Italic, contentDescription = "Italic", tint = NoteySurfaces.TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { insertMarkdown("### ") }, modifier = Modifier.size(34.dp)) {
                            Icon(NoteyIcons.Heading, contentDescription = "Heading", tint = NoteySurfaces.TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { insertMarkdown("- ") }, modifier = Modifier.size(34.dp)) {
                            Icon(NoteyIcons.BulletList, contentDescription = "Bullet List", tint = NoteySurfaces.TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { insertMarkdown("- [ ] ") }, modifier = Modifier.size(34.dp)) {
                            Icon(NoteyIcons.TaskCheck, contentDescription = "Task Checkbox", tint = NoteySurfaces.TextPrimary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { insertMarkdown("\n```\n", "\n```\n") }, modifier = Modifier.size(34.dp)) {
                            Icon(NoteyIcons.Code, contentDescription = "Code Block", tint = NoteySurfaces.TextPrimary, modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showThemeSheet = true }, modifier = Modifier.size(34.dp)) {
                            Icon(NoteyIcons.Palette, contentDescription = "Theme", tint = accentColor, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.size(34.dp)) {
                            Icon(NoteyIcons.Paperclip, contentDescription = "Attach File", tint = NoteySurfaces.TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        containerColor = NoteySurfaces.Surface0
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Borderless Title
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    color = NoteySurfaces.TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                ),
                cursorBrush = SolidColor(accentColor),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (title.isBlank()) {
                        Text(
                            text = "Title",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = NoteySurfaces.TextMuted
                        )
                    }
                    innerTextField()
                }
            )

            // Monospace Metadata Ribbon
            val wordCount = remember(bodyTextFieldValue.text, tasks, isChecklist) {
                val fullText = if (isChecklist) tasks.joinToString(" ") { it.text } else bodyTextFieldValue.text
                fullText.split(Regex("\\s+")).count { it.isNotBlank() }
            }
            val charCount = if (isChecklist) tasks.sumOf { it.text.length } else bodyTextFieldValue.text.length
            val readTime = (wordCount / 200).coerceAtLeast(1)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$wordCount words • $charCount chars • $readTime min read",
                    style = MonoMetadataStyle
                )
                if (folder.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor
                    ) {
                        Text(
                            text = folder,
                            style = MonoMetadataStyle,
                            fontWeight = FontWeight.Bold,
                            color = getInvertedTextColor(accentColor),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = NoteySurfaces.HairlineBorder)

            // Checklist Mode vs Text Mode
            if (isChecklist) {
                val activeTasks = tasks.mapIndexed { idx, t -> Pair(idx, t) }.filter { !it.second.isDone }
                val completedTasks = tasks.mapIndexed { idx, t -> Pair(idx, t) }.filter { it.second.isDone }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    activeTasks.forEach { (taskIdx, task) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = task.isDone,
                                onCheckedChange = { checked ->
                                    tasks = tasks.toMutableList().also { list ->
                                        list[taskIdx] = task.copy(isDone = checked)
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentColor,
                                    uncheckedColor = NoteySurfaces.TextSecondary.copy(alpha = 0.5f),
                                    checkmarkColor = getInvertedTextColor(accentColor)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            BasicTextField(
                                value = task.text,
                                onValueChange = { newText ->
                                    tasks = tasks.toMutableList().also { list ->
                                        list[taskIdx] = task.copy(text = newText)
                                    }
                                },
                                textStyle = TextStyle(color = NoteySurfaces.TextPrimary, fontSize = 16.sp),
                                cursorBrush = SolidColor(accentColor),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = {
                                    tasks = tasks.toMutableList().also { list ->
                                        list.add(taskIdx + 1, TaskItem(text = "", isDone = false))
                                    }
                                }),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (task.text.isBlank()) {
                                        Text("List item", color = NoteySurfaces.TextMuted, fontSize = 16.sp)
                                    }
                                    inner()
                                }
                            )
                            IconButton(
                                onClick = {
                                    tasks = tasks.toMutableList().also { it.removeAt(taskIdx) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = NoteySurfaces.TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Add new item row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                tasks = tasks + TaskItem(text = "", isDone = false)
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Add checklist item", color = NoteySurfaces.TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                // Completed Items Section
                if (completedTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = NoteySurfaces.HairlineBorder)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCompletedTasks = !showCompletedTasks }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            if (showCompletedTasks) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = NoteySurfaces.TextSecondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${completedTasks.size} Completed ${if (completedTasks.size == 1) "item" else "items"}",
                            style = MonoMetadataStyle,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (showCompletedTasks) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            completedTasks.forEach { (taskIdx, task) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = task.isDone,
                                        onCheckedChange = { checked ->
                                            tasks = tasks.toMutableList().also { list ->
                                                list[taskIdx] = task.copy(isDone = checked)
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = accentColor,
                                            uncheckedColor = NoteySurfaces.TextSecondary.copy(alpha = 0.5f),
                                            checkmarkColor = getInvertedTextColor(accentColor)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = task.text,
                                        style = TextStyle(
                                            color = NoteySurfaces.TextMuted,
                                            fontSize = 16.sp,
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            tasks = tasks.toMutableList().also { it.removeAt(taskIdx) }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = NoteySurfaces.TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Plain Text Mode (Borderless auto-expanding)
                BasicTextField(
                    value = bodyTextFieldValue,
                    onValueChange = { bodyTextFieldValue = it },
                    textStyle = TextStyle(
                        color = NoteySurfaces.TextPrimary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp),
                    decorationBox = { innerTextField ->
                        if (bodyTextFieldValue.text.isBlank()) {
                            Text(
                                text = "Start writing with markdown...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = NoteySurfaces.TextMuted
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // Attachments List in Editor
            if (attachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("ATTACHMENTS (${attachments.size})", style = MonoMetadataStyle, fontWeight = FontWeight.Bold)
                attachments.forEach { att ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NoteySurfaces.Surface1,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(NoteyIcons.Paperclip, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onPreviewMedia?.invoke(att) }
                            ) {
                                Text(att.displayName, modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis, color = NoteySurfaces.TextPrimary)
                                val sizeKb = (att.sizeBytes / 1024).coerceAtLeast(1)
                                Text("$sizeKb KB • ${att.mimeType}", style = MonoMetadataStyle)
                            }
                            IconButton(onClick = { attachments = attachments.filter { it.id != att.id } }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Theme & Accent Modal Bottom Sheet
    if (showThemeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = NoteySurfaces.Surface2
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp, start = 18.dp, end = 18.dp)
            ) {
                Text(
                    text = "ACCENT COLOR",
                    style = MonoMetadataStyle,
                    fontWeight = FontWeight.Bold,
                    color = NoteySurfaces.TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GoogleKeepPalette.colors.forEach { keepColor ->
                        val isSelected = selectedColorIdx == keepColor.id
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(keepColor.accentColor)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else NoteySurfaces.HairlineBorder,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIdx = keepColor.id },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = getInvertedTextColor(keepColor.accentColor),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "TEXTURE OVERLAY",
                    style = MonoMetadataStyle,
                    fontWeight = FontWeight.Bold,
                    color = NoteySurfaces.TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("none", "grid", "dots", "ruled").forEach { themeId ->
                        val isSelected = selectedBgTheme == themeId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) NoteySurfaces.Surface3 else NoteySurfaces.Surface1,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) accentColor else NoteySurfaces.HairlineBorder
                            ),
                            modifier = Modifier
                                .width(88.dp)
                                .height(52.dp)
                                .clickable { selectedBgTheme = themeId }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = themeId.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) accentColor else NoteySurfaces.TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFolderDialog) {
        var folderInput by remember { mutableStateOf(folder) }
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            containerColor = NoteySurfaces.Surface2,
            title = { Text("Set Folder / Collection", color = NoteySurfaces.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = folderInput,
                    onValueChange = { folderInput = it },
                    label = { Text("Folder Name (e.g. Work, Ideas)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        folder = folderInput.trim()
                        showFolderDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExportSheet) {
        val finalBody = if (isChecklist) {
            tasks.filter { it.text.isNotBlank() }.joinToString("\n") { task ->
                val pfx = if (task.isDone) "- [x] " else "- [ ] "
                "$pfx${task.text}"
            }
        } else {
            bodyTextFieldValue.text
        }
        ExportShareBottomSheet(
            title = title.ifBlank { "Untitled Note" },
            content = finalBody,
            onDismiss = { showExportSheet = false },
            onSavedSuccessfully = { showExportSheet = false }
        )
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
            .background(NoteySurfaces.Surface0)
    ) {
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search workspace...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = NoteySurfaces.HairlineBorder,
                        focusedTextColor = NoteySurfaces.TextPrimary,
                        unfocusedTextColor = NoteySurfaces.TextPrimary
                    )
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = NoteySurfaces.Surface0,
                navigationIconContentColor = NoteySurfaces.TextPrimary
            ),
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
                val isSelected = scope == s
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) NoteySurfaces.Surface3 else NoteySurfaces.Surface1,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF38BDF8) else NoteySurfaces.HairlineBorder
                    ),
                    modifier = Modifier.clickable { scope = s }
                ) {
                    Text(
                        text = s,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) Color(0xFF38BDF8) else NoteySurfaces.TextSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = NoteySurfaces.HairlineBorder)

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
                        shape = RoundedCornerShape(12.dp),
                        color = NoteySurfaces.Surface1,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCanvas(doc.id) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(NoteyIcons.Workflow, contentDescription = null, tint = Color(0xFFA78BFA), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(doc.name, fontWeight = FontWeight.Bold, color = NoteySurfaces.TextPrimary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${doc.nodes.size} nodes • ${doc.type.name.lowercase()}", style = MonoMetadataStyle)
                        }
                    }
                }
            }
        }
    }
}

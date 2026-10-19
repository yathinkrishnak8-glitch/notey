package com.pocketdev.notey

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = NoteStore(applicationContext)
        setContent {
            NoteyApp(store)
        }
    }
}

// -------------------------------------------------------------
// Theming & Liquid Glass Color Palettes
// -------------------------------------------------------------

enum class GlassTheme(
    val title: String,
    val bgGradients: List<Color>,
    val accentColor: Color,
    val surfaceColor: Color,
    val onSurfaceColor: Color,
    val isDark: Boolean,
) {
    AMETHYST(
        title = "Amethyst Glow",
        bgGradients = listOf(Color(0xFF0F051D), Color(0xFF2B1055), Color(0xFF090214)),
        accentColor = Color(0xFFD47AFF),
        surfaceColor = Color(0x33441B78),
        onSurfaceColor = Color(0xFFF3E8FF),
        isDark = true,
    ),
    AURORA(
        title = "Aurora Borealis",
        bgGradients = listOf(Color(0xFF03161C), Color(0xFF08414F), Color(0xFF020E12)),
        accentColor = Color(0xFF38EF7D),
        surfaceColor = Color(0x33095364),
        onSurfaceColor = Color(0xFFE0FBF6),
        isDark = true,
    ),
    SUNSET(
        title = "Sunset Ember",
        bgGradients = listOf(Color(0xFF1E0A16), Color(0xFF4C182B), Color(0xFF13040B)),
        accentColor = Color(0xFFFF758C),
        surfaceColor = Color(0x33681F39),
        onSurfaceColor = Color(0xFFFFECEF),
        isDark = true,
    ),
    OBSIDIAN(
        title = "Midnight Obsidian",
        bgGradients = listOf(Color(0xFF080B10), Color(0xFF101929), Color(0xFF040609)),
        accentColor = Color(0xFF00D2FF),
        surfaceColor = Color(0x331E293B),
        onSurfaceColor = Color(0xFFF1F5F9),
        isDark = true,
    ),
    FROST_LIGHT(
        title = "Frost Light",
        bgGradients = listOf(Color(0xFFE9F1F5), Color(0xFFD6E4EE), Color(0xFFF3F7FA)),
        accentColor = Color(0xFF3B5998),
        surfaceColor = Color(0x66FFFFFF),
        onSurfaceColor = Color(0xFF1E293B),
        isDark = false,
    );
}

val NOTE_ACCENT_COLORS = listOf(
    Color(0x00000000), // Default neutral glass
    Color(0x338A2BE2), // Amethyst
    Color(0x3300D2FF), // Ice Cyan
    Color(0x332ECC71), // Emerald
    Color(0x33F39C12), // Amber
    Color(0x33E74C3C), // Ruby
    Color(0x33FD79A8), // Rose
)

val NOTE_ACCENT_NAMES = listOf(
    "Glass Neutral", "Amethyst", "Sapphire", "Emerald", "Amber", "Ruby", "Rose"
)

// -------------------------------------------------------------
// Screen Navigation State
// -------------------------------------------------------------

sealed interface Screen {
    data object List : Screen
    data class Preview(val noteId: Long) : Screen
    data class Edit(val noteId: Long?) : Screen // null = brand new note
}

// -------------------------------------------------------------
// Root App Composable
// -------------------------------------------------------------

@Composable
fun NoteyApp(store: NoteStore) {
    var currentThemeIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentTheme = GlassTheme.entries[currentThemeIndex % GlassTheme.entries.size]

    var notes by remember { mutableStateOf(store.loadAll()) }
    var currentScreen by rememberSaveable { mutableStateOf<String>("list") }
    var selectedNoteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var activePopupAttachment by remember { mutableStateOf<Attachment?>(null) }

    fun refreshNotes() {
        notes = store.loadAll()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(currentTheme.bgGradients)
            )
    ) {
        when (currentScreen) {
            "list" -> {
                NoteListScreen(
                    notes = notes,
                    theme = currentTheme,
                    onThemeChange = {
                        currentThemeIndex = (currentThemeIndex + 1) % GlassTheme.entries.size
                    },
                    onOpenNote = { note ->
                        selectedNoteId = note.id
                        currentScreen = "preview"
                    },
                    onCreateNote = {
                        selectedNoteId = null
                        currentScreen = "edit"
                    },
                    onTogglePin = { note ->
                        store.togglePin(note.id)
                        refreshNotes()
                    },
                    onDeleteNote = { note ->
                        store.delete(note.id)
                        refreshNotes()
                    },
                    onOpenAttachment = { att ->
                        activePopupAttachment = att
                    }
                )
            }
            "preview" -> {
                val note = notes.firstOrNull { it.id == selectedNoteId }
                if (note == null) {
                    currentScreen = "list"
                } else {
                    NotePreviewScreen(
                        note = note,
                        theme = currentTheme,
                        onBack = { currentScreen = "list" },
                        onEdit = { currentScreen = "edit" },
                        onTogglePin = {
                            store.togglePin(note.id)
                            refreshNotes()
                        },
                        onDelete = {
                            store.delete(note.id)
                            refreshNotes()
                            currentScreen = "list"
                        },
                        onOpenAttachment = { att ->
                            activePopupAttachment = att
                        }
                    )
                }
            }
            "edit" -> {
                val existing = notes.firstOrNull { it.id == selectedNoteId }
                NoteEditScreen(
                    initialNote = existing,
                    theme = currentTheme,
                    store = store,
                    onSave = { updated ->
                        store.save(updated)
                        refreshNotes()
                        selectedNoteId = updated.id
                        currentScreen = "preview"
                    },
                    onDiscard = {
                        if (existing != null) {
                            currentScreen = "preview"
                        } else {
                            currentScreen = "list"
                        }
                    },
                    onDelete = {
                        if (existing != null) {
                            store.delete(existing.id)
                            refreshNotes()
                        }
                        currentScreen = "list"
                    },
                    onOpenAttachment = { att ->
                        activePopupAttachment = att
                    }
                )
            }
        }

        // Active popup player / viewer dialog
        activePopupAttachment?.let { attachment ->
            MediaPopupViewer(
                attachment = attachment,
                onDismiss = { activePopupAttachment = null }
            )
        }
    }
}

// -------------------------------------------------------------
// Liquid Glass Reusable Card
// -------------------------------------------------------------

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = Color.Transparent,
    surfaceAlpha: Float = 0.40f,
    borderAlpha: Float = 0.25f,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val cardModifier = if (onClick != null) {
        modifier.clip(shape).clickable(onClick = onClick)
    } else {
        modifier.clip(shape)
    }

    Box(
        modifier = cardModifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = surfaceAlpha),
                        Color.White.copy(alpha = surfaceAlpha * 0.4f)
                    )
                )
            )
            .then(
                if (accentColor != Color.Transparent) {
                    Modifier.background(accentColor)
                } else Modifier
            )
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = borderAlpha),
                            Color.White.copy(alpha = borderAlpha * 0.2f)
                        )
                    )
                ),
                shape = shape
            )
            .padding(16.dp)
    ) {
        content()
    }
}

// -------------------------------------------------------------
// 1. Note List Screen (Home)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    notes: List<Note>,
    theme: GlassTheme,
    onThemeChange: () -> Unit,
    onOpenNote: (Note) -> Unit,
    onCreateNote: () -> Unit,
    onTogglePin: (Note) -> Unit,
    onDeleteNote: (Note) -> Unit,
    onOpenAttachment: (Attachment) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) notes
        else notes.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.body.contains(searchQuery, ignoreCase = true) ||
                    it.attachments.any { a -> a.displayName.contains(searchQuery, ignoreCase = true) }
        }
    }

    val pinnedNotes = remember(filteredNotes) { filteredNotes.filter { it.isPinned } }
    val otherNotes = remember(filteredNotes) { filteredNotes.filter { !it.isPinned } }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(36.dp))

            // Glass Header & Theme Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "notey",
                        color = theme.onSurfaceColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "${notes.size} ${if (notes.size == 1) "note" else "notes"} • ${theme.title}",
                        color = theme.onSurfaceColor.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }

                // Theme switch button with glowing glass pill
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable(onClick = onThemeChange)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(theme.accentColor)
                    )
                    Text("Theme", color = theme.onSurfaceColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Glass Search Bar
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceAlpha = 0.15f,
                borderAlpha = 0.2f
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = theme.onSurfaceColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Search notes, media, files...",
                                color = theme.onSurfaceColor.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = theme.onSurfaceColor,
                            unfocusedTextColor = theme.onSurfaceColor,
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = theme.onSurfaceColor.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Notes List
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LiquidGlassCard(
                        modifier = Modifier.padding(24.dp),
                        surfaceAlpha = 0.15f
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text("✨", fontSize = 36.sp)
                            Text(
                                if (searchQuery.isBlank()) "No notes yet" else "No matching notes",
                                color = theme.onSurfaceColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                if (searchQuery.isBlank()) "Tap + to create your first liquid glass note!"
                                else "Try a different search keyword",
                                color = theme.onSurfaceColor.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pinned Notes Header & Section
                    if (pinnedNotes.isNotEmpty()) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text("📌", fontSize = 14.sp)
                                Text(
                                    "PINNED NOTES",
                                    color = theme.accentColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        items(pinnedNotes, key = { "pinned_${it.id}" }) { note ->
                            NoteCardItem(
                                note = note,
                                theme = theme,
                                onClick = { onOpenNote(note) },
                                onTogglePin = { onTogglePin(note) },
                                onDelete = { onDeleteNote(note) },
                                onOpenAttachment = onOpenAttachment
                            )
                        }

                        if (otherNotes.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text("📝", fontSize = 14.sp)
                                    Text(
                                        "OTHER NOTES",
                                        color = theme.onSurfaceColor.copy(alpha = 0.6f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }

                    // Remaining Notes
                    items(otherNotes, key = { it.id }) { note ->
                        NoteCardItem(
                            note = note,
                            theme = theme,
                            onClick = { onOpenNote(note) },
                            onTogglePin = { onTogglePin(note) },
                            onDelete = { onDeleteNote(note) },
                            onOpenAttachment = onOpenAttachment
                        )
                    }
                }
            }
        }

        // Floating Action Button for New Note
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            FloatingActionButton(
                onClick = onCreateNote,
                containerColor = theme.accentColor,
                contentColor = if (theme.isDark) Color.Black else Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(62.dp)
                    .shadow(12.dp, CircleShape, spotColor = theme.accentColor)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Create note", modifier = Modifier.size(30.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// Note Card Item Component
// -------------------------------------------------------------

@Composable
fun NoteCardItem(
    note: Note,
    theme: GlassTheme,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onOpenAttachment: (Attachment) -> Unit,
) {
    val accentTint = NOTE_ACCENT_COLORS.getOrElse(note.colorIndex) { Color.Transparent }

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        accentColor = accentTint,
        surfaceAlpha = if (theme.isDark) 0.18f else 0.45f,
        borderAlpha = if (note.isPinned) 0.55f else 0.25f,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row: Pin & Title & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (note.isPinned) {
                        Text("📌", fontSize = 13.sp)
                    }
                    Text(
                        text = note.title.ifBlank { "(Untitled note)" },
                        color = theme.onSurfaceColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick Pin Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(32.dp)) {
                        Text(if (note.isPinned) "📌" else "📍", fontSize = 14.sp)
                    }
                }
            }

            // Note Body Preview
            if (note.body.isNotBlank()) {
                Text(
                    text = note.body,
                    color = theme.onSurfaceColor.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            // Attachments Preview Row
            if (note.attachments.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    items(note.attachments, key = { it.id }) { att ->
                        AttachmentThumbnailChip(
                            attachment = att,
                            theme = theme,
                            onClick = { onOpenAttachment(att) }
                        )
                    }
                }
            }

            // Footer Timestamp & Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(Date(note.updatedAt)),
                    color = theme.onSurfaceColor.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )

                if (note.attachments.isNotEmpty()) {
                    Text(
                        text = "${note.attachments.size} ${if (note.attachments.size == 1) "attachment" else "attachments"}",
                        color = theme.accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. Note Preview Screen (Reader Mode)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotePreviewScreen(
    note: Note,
    theme: GlassTheme,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onOpenAttachment: (Attachment) -> Unit,
) {
    val context = LocalContext.current
    BackHandler { onBack() }

    val accentTint = NOTE_ACCENT_COLORS.getOrElse(note.colorIndex) { Color.Transparent }
    val wordCount = remember(note.body) {
        if (note.body.isBlank()) 0 else note.body.trim().split("\\s+".toRegex()).size
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(36.dp))

            // Glass Top Bar with Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .size(42.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = theme.onSurfaceColor
                    )
                }

                // Actions: Pin, Share, Delete, Edit
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Pin
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (note.isPinned) theme.accentColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.15f))
                            .size(42.dp)
                    ) {
                        Text(if (note.isPinned) "📌" else "📍", fontSize = 16.sp)
                    }

                    // Share
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, note.title)
                                putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.body}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .size(42.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = theme.onSurfaceColor)
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x33E74C3C))
                            .size(42.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFFF6B6B))
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Reader Glass Container
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = accentTint,
                surfaceAlpha = if (theme.isDark) 0.22f else 0.50f
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Title
                    Text(
                        text = note.title.ifBlank { "(Untitled Note)" },
                        color = theme.onSurfaceColor,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )

                    // Meta Information Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                .format(Date(note.updatedAt)),
                            color = theme.onSurfaceColor.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Word count badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "$wordCount words",
                                    color = theme.onSurfaceColor.copy(alpha = 0.75f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Media Gallery (if any)
                    if (note.attachments.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "ATTACHMENTS (${note.attachments.size})",
                                color = theme.accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )

                            // Media preview grid / row
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(note.attachments, key = { it.id }) { att ->
                                    MediaGalleryCard(
                                        attachment = att,
                                        theme = theme,
                                        onClick = { onOpenAttachment(att) }
                                    )
                                }
                            }
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    // Note Body
                    if (note.body.isNotBlank()) {
                        Text(
                            text = note.body,
                            color = theme.onSurfaceColor,
                            fontSize = 16.sp,
                            lineHeight = 26.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    } else {
                        Text(
                            text = "(Empty note body)",
                            color = theme.onSurfaceColor.copy(alpha = 0.4f),
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }

        // Floating Action Button to Edit
        FloatingActionButton(
            onClick = onEdit,
            containerColor = theme.accentColor,
            contentColor = if (theme.isDark) Color.Black else Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(60.dp)
                .shadow(12.dp, CircleShape, spotColor = theme.accentColor)
        ) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit note", modifier = Modifier.size(26.dp))
        }
    }
}

// -------------------------------------------------------------
// 3. Note Edit Screen (Editor Mode)
// -------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    initialNote: Note?,
    theme: GlassTheme,
    store: NoteStore,
    onSave: (Note) -> Unit,
    onDiscard: () -> Unit,
    onDelete: () -> Unit,
    onOpenAttachment: (Attachment) -> Unit,
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf(initialNote?.title ?: "") }
    var body by rememberSaveable { mutableStateOf(initialNote?.body ?: "") }
    var isPinned by rememberSaveable { mutableStateOf(initialNote?.isPinned ?: false) }
    var colorIndex by rememberSaveable { mutableIntStateOf(initialNote?.colorIndex ?: 0) }
    var attachments by remember { mutableStateOf(initialNote?.attachments ?: emptyList()) }

    // Pickers
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val att = store.copyUriToInternalStorage(it)
            if (att != null) attachments = attachments + att
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val att = store.copyUriToInternalStorage(it)
            if (att != null) attachments = attachments + att
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val att = store.copyUriToInternalStorage(it)
            if (att != null) attachments = attachments + att
        }
    }

    fun doSave() {
        if (title.isNotBlank() || body.isNotBlank() || attachments.isNotEmpty()) {
            val id = initialNote?.id ?: System.currentTimeMillis()
            val note = Note(
                id = id,
                title = title.trim(),
                body = body,
                isPinned = isPinned,
                colorIndex = colorIndex,
                attachments = attachments,
                updatedAt = System.currentTimeMillis()
            )
            onSave(note)
        } else {
            onDiscard()
        }
    }

    BackHandler { doSave() }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(36.dp))

            // Glass Top Bar with Save & Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back & Auto-save button
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { doSave() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Save and Back",
                        tint = theme.onSurfaceColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Save", color = theme.onSurfaceColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Action buttons: Pin toggle, Delete
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isPinned) theme.accentColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.15f))
                            .size(38.dp)
                    ) {
                        Text(if (isPinned) "📌" else "📍", fontSize = 14.sp)
                    }

                    if (initialNote != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0x33E74C3C))
                                .size(38.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFFF6B6B))
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Accent Color Tint Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tint:", color = theme.onSurfaceColor.copy(alpha = 0.7f), fontSize = 12.sp)
                NOTE_ACCENT_COLORS.forEachIndexed { index, color ->
                    val isSelected = colorIndex == index
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (color == Color.Transparent) Color.White.copy(alpha = 0.2f) else color.copy(alpha = 0.8f))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) theme.accentColor else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { colorIndex = index }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Title Field
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceAlpha = 0.18f
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = {
                        Text("Note Title...", color = theme.onSurfaceColor.copy(alpha = 0.5f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = theme.onSurfaceColor,
                        unfocusedTextColor = theme.onSurfaceColor,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(10.dp))

            // Attached Media Strip (with Remove Button)
            if (attachments.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(attachments, key = { it.id }) { att ->
                        Box(contentAlignment = Alignment.TopEnd) {
                            AttachmentThumbnailChip(
                                attachment = att,
                                theme = theme,
                                onClick = { onOpenAttachment(att) }
                            )
                            // Remove button
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .clickable {
                                        attachments = attachments.filter { it.id != att.id }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            // Note Body Editor Field
            LiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                surfaceAlpha = 0.18f
            ) {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    placeholder = {
                        Text("Write your note here...", color = theme.onSurfaceColor.copy(alpha = 0.4f), fontSize = 15.sp)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = theme.onSurfaceColor,
                        unfocusedTextColor = theme.onSurfaceColor,
                    ),
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(8.dp))

            // Attachment Floating Action Bar at the Bottom
            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                surfaceAlpha = 0.25f,
                borderAlpha = 0.35f
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Add Photo
                    ActionButtonPill(
                        icon = "📷",
                        label = "Photo",
                        theme = theme,
                        onClick = { photoPicker.launch("image/*") }
                    )

                    // Add Video
                    ActionButtonPill(
                        icon = "🎥",
                        label = "Video",
                        theme = theme,
                        onClick = { videoPicker.launch("video/*") }
                    )

                    // Add Document
                    ActionButtonPill(
                        icon = "📄",
                        label = "File",
                        theme = theme,
                        onClick = { filePicker.launch("*/*") }
                    )

                    // Preview Button
                    ActionButtonPill(
                        icon = "👁️",
                        label = "Preview",
                        theme = theme,
                        onClick = { doSave() }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ActionButtonPill(
    icon: String,
    label: String,
    theme: GlassTheme,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(icon, fontSize = 16.sp)
        Text(label, color = theme.onSurfaceColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// -------------------------------------------------------------
// Attachment Thumbnail & Gallery Components
// -------------------------------------------------------------

@Composable
fun AttachmentThumbnailChip(
    attachment: Attachment,
    theme: GlassTheme,
    onClick: () -> Unit,
) {
    val bitmapState = produceState<Bitmap?>(initialValue = null, attachment.filePath) {
        if (attachment.type == AttachmentType.IMAGE) {
            value = withContext(Dispatchers.IO) {
                loadDownsampledBitmap(attachment.filePath, 160, 160)
            }
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val bmp = bitmapState.value
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = attachment.displayName,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = when (attachment.type) {
                        AttachmentType.IMAGE -> "🖼️"
                        AttachmentType.VIDEO -> "🎬"
                        AttachmentType.FILE -> "📄"
                    },
                    fontSize = 16.sp
                )
            }
            Text(
                text = attachment.displayName,
                color = theme.onSurfaceColor,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(100.dp)
            )
        }
    }
}

@Composable
fun MediaGalleryCard(
    attachment: Attachment,
    theme: GlassTheme,
    onClick: () -> Unit,
) {
    val bitmapState = produceState<Bitmap?>(initialValue = null, attachment.filePath) {
        if (attachment.type == AttachmentType.IMAGE) {
            value = withContext(Dispatchers.IO) {
                loadDownsampledBitmap(attachment.filePath, 320, 320)
            }
        }
    }

    Box(
        modifier = Modifier
            .size(130.dp, 110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmapState.value
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = attachment.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = when (attachment.type) {
                        AttachmentType.IMAGE -> "🖼️"
                        AttachmentType.VIDEO -> "▶️"
                        AttachmentType.FILE -> "📄"
                    },
                    fontSize = 32.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = attachment.displayName,
                    color = theme.onSurfaceColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatFileSize(attachment.sizeBytes),
                    color = theme.onSurfaceColor.copy(alpha = 0.6f),
                    fontSize = 9.sp
                )
            }
        }

        // Overlay badge for Video
        if (attachment.type == AttachmentType.VIDEO) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// 4. Modal Popup Media & Video Player
// -------------------------------------------------------------

@Composable
fun MediaPopupViewer(
    attachment: Attachment,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF141A28))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {} // Prevent click-through dismiss
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header: Title & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachment.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${attachment.type} • ${formatFileSize(attachment.sizeBytes)}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .size(36.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Media Content Area
                when (attachment.type) {
                    AttachmentType.IMAGE -> {
                        val fullBitmap = produceState<Bitmap?>(initialValue = null, attachment.filePath) {
                            value = withContext(Dispatchers.IO) {
                                loadDownsampledBitmap(attachment.filePath, 1200, 1200)
                            }
                        }

                        val bmp = fullBitmap.value
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = attachment.displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 420.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = Color(0xFF00D2FF))
                            }
                        }
                    }
                    AttachmentType.VIDEO -> {
                        // Video Player using framework VideoView
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        setVideoPath(attachment.filePath)
                                        val mediaController = MediaController(ctx)
                                        mediaController.setAnchorView(this)
                                        setMediaController(mediaController)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    AttachmentType.FILE -> {
                        // Generic File Viewer / Launch with intent
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text("📄", fontSize = 48.sp)
                            Text(
                                "Document File",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                attachment.filePath,
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Open with system intent button
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF00D2FF))
                                    .clickable {
                                        try {
                                            val file = File(attachment.filePath)
                                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(Uri.fromFile(file), attachment.mimeType)
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(viewIntent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text("Open with Application", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Bitmap and File Utility Helpers
// -------------------------------------------------------------

fun loadDownsampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)

        var inSampleSize = 1
        if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
            val halfHeight: Int = options.outHeight / 2
            val halfWidth: Int = options.outWidth / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        BitmapFactory.decodeFile(path, decodeOptions)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.1f KB", kb)
        else -> "$bytes B"
    }
}

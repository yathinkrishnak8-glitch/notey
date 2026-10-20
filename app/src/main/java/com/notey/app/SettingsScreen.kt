package com.notey.app

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

enum class SettingsSubpage {
    MAIN, PROFILE, APPEARANCE, ANIMATION, NOTES, CANVAS, STORAGE, PERFORMANCE, DATA, ABOUT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onSettingsChanged: (AppSettings) -> Unit,
    noteStore: NoteStore,
    userProfileStore: UserProfileStore? = null,
    currentProfile: UserProfile? = null,
    onProfileChanged: ((UserProfile) -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var currentSubpage by remember { mutableStateOf(SettingsSubpage.MAIN) }
    var showConfirmDeleteAll by remember { mutableStateOf(false) }

    fun update(newSettings: AppSettings) {
        onSettingsChanged(newSettings)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoteySurfaces.Surface0)
    ) {
        // Top App Bar
        Surface(
            color = NoteySurfaces.Surface1,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentSubpage != SettingsSubpage.MAIN) {
                    IconButton(onClick = { currentSubpage = SettingsSubpage.MAIN }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NoteySurfaces.TextPrimary)
                    }
                } else if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NoteySurfaces.TextPrimary)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when (currentSubpage) {
                        SettingsSubpage.MAIN -> "Settings"
                        SettingsSubpage.PROFILE -> "User Profile"
                        SettingsSubpage.APPEARANCE -> "Appearance"
                        SettingsSubpage.ANIMATION -> "Animation & Physics"
                        SettingsSubpage.NOTES -> "Notes & Editing"
                        SettingsSubpage.CANVAS -> "Canvas & Workflows"
                        SettingsSubpage.STORAGE -> "Files & Storage"
                        SettingsSubpage.PERFORMANCE -> "Performance"
                        SettingsSubpage.DATA -> "Data & Privacy"
                        SettingsSubpage.ABOUT -> "About Notey"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NoteySurfaces.TextPrimary
                )
            }
        }

        // Subpage Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentSubpage) {
                SettingsSubpage.MAIN -> MainSettingsListView(
                    currentProfile = currentProfile,
                    settings = settings,
                    onNavigate = { currentSubpage = it }
                )
                SettingsSubpage.PROFILE -> ProfileSettingsView(
                    currentProfile = currentProfile,
                    onSaveProfile = {
                        userProfileStore?.saveProfile(it)
                        onProfileChanged?.invoke(it)
                        currentSubpage = SettingsSubpage.MAIN
                    },
                    onCancel = { currentSubpage = SettingsSubpage.MAIN }
                )
                SettingsSubpage.APPEARANCE -> AppearanceSettingsView(
                    settings = settings,
                    onUpdate = ::update
                )
                SettingsSubpage.ANIMATION -> AnimationSettingsView(
                    settings = settings,
                    onUpdate = ::update
                )
                SettingsSubpage.NOTES -> NotesSettingsView(
                    settings = settings,
                    onUpdate = ::update
                )
                SettingsSubpage.CANVAS -> CanvasSettingsView(
                    settings = settings,
                    onUpdate = ::update
                )
                SettingsSubpage.STORAGE -> StorageSettingsView(
                    noteStore = noteStore,
                    context = context
                )
                SettingsSubpage.PERFORMANCE -> PerformanceSettingsView(settings = settings)
                SettingsSubpage.DATA -> DataPrivacySettingsView(
                    noteStore = noteStore,
                    onRequestDeleteAll = { showConfirmDeleteAll = true }
                )
                SettingsSubpage.ABOUT -> AboutSettingsView(context = context)
            }
        }
    }

    // Confirmation dialog for deleting all data
    if (showConfirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteAll = false },
            title = { Text("Delete All Data?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently erase all notes, tasks, attached files, and visual canvases. This action cannot be reversed.") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDeleteAll = false
                        noteStore.clearTrash()
                        val all = noteStore.loadAll()
                        all.forEach { noteStore.permanentlyDelete(it.id) }
                        Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDeleteAll = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MainSettingsListView(
    currentProfile: UserProfile?,
    settings: AppSettings,
    onNavigate: (SettingsSubpage) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // User Profile Banner Card
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NoteySurfaces.Surface1,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(SettingsSubpage.PROFILE) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = UserProfileValidator.getInitials(currentProfile?.displayName ?: "Guest"),
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentProfile?.displayName ?: "Set Up Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NoteySurfaces.TextPrimary
                    )
                    Text(
                        text = "Local-First Workspace • Tap to edit",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = NoteySurfaces.TextMuted
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        SettingsCategoryCard(
            icon = NoteyIcons.Palette,
            title = "Appearance",
            subtitle = "AMOLED Black, Zinc Grey, 6 dynamic accents",
            onClick = { onNavigate(SettingsSubpage.APPEARANCE) }
        )

        SettingsCategoryCard(
            icon = Icons.Default.PlayArrow,
            title = "Animation & Physics",
            subtitle = "Instant, Fluid 240ms, Smooth Spring",
            onClick = { onNavigate(SettingsSubpage.ANIMATION) }
        )

        SettingsCategoryCard(
            icon = NoteyIcons.Document,
            title = "Notes & Editing",
            subtitle = "Default styling, sorting, and auto-save options",
            onClick = { onNavigate(SettingsSubpage.NOTES) }
        )

        SettingsCategoryCard(
            icon = NoteyIcons.Workflow,
            title = "Canvas & Workflows",
            subtitle = "Grid visibility, dot spacing, connectors",
            onClick = { onNavigate(SettingsSubpage.CANVAS) }
        )

        SettingsCategoryCard(
            icon = NoteyIcons.Vault,
            title = "Files & Storage",
            subtitle = "Storage stats, cache usage, backup & restore",
            onClick = { onNavigate(SettingsSubpage.STORAGE) }
        )

        SettingsCategoryCard(
            icon = Icons.Default.Settings,
            title = "Performance",
            subtitle = "Local-first sandboxing, hardware acceleration",
            onClick = { onNavigate(SettingsSubpage.PERFORMANCE) }
        )

        SettingsCategoryCard(
            icon = Icons.Default.Lock,
            title = "Data & Privacy",
            subtitle = "Local storage management, trash, data deletion",
            onClick = { onNavigate(SettingsSubpage.DATA) }
        )

        SettingsCategoryCard(
            icon = Icons.Default.Info,
            title = "About Notey",
            subtitle = "Built by Yathin • Source code & attribution",
            onClick = { onNavigate(SettingsSubpage.ABOUT) }
        )
    }
}

@Composable
fun SettingsCategoryCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = NoteySurfaces.Surface1,
        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
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
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NoteySurfaces.Surface2)
                    .border(1.dp, NoteySurfaces.HairlineBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NoteySurfaces.TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NoteySurfaces.TextSecondary
                )
            }
        }
    }
}

@Composable
fun ProfileSettingsView(
    currentProfile: UserProfile?,
    onSaveProfile: (UserProfile) -> Unit,
    onCancel: () -> Unit
) {
    var nameInput by remember { mutableStateOf(currentProfile?.displayName ?: "") }
    var avatarUri by remember { mutableStateOf(currentProfile?.avatarUri) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            avatarUri = uri.toString()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Profile Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NoteySurfaces.TextPrimary)

        OutlinedTextField(
            value = nameInput,
            onValueChange = {
                nameInput = it
                errorMessage = null
            },
            label = { Text("Display Name") },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                Icon(NoteyIcons.Camera, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Choose Photo")
            }
            if (avatarUri != null) {
                TextButton(onClick = { avatarUri = null }) {
                    Text("Clear")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val err = UserProfileValidator.validateDisplayName(nameInput)
                    if (err != null) {
                        errorMessage = err
                    } else {
                        onSaveProfile(
                            currentProfile?.copy(
                                displayName = nameInput.trim(),
                                avatarUri = avatarUri,
                                updatedAt = System.currentTimeMillis()
                            ) ?: UserProfile(
                                displayName = nameInput.trim(),
                                avatarUri = avatarUri
                            )
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Profile")
            }
        }
    }
}

@Composable
fun AppearanceSettingsView(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit
) {
    val currentAccent = AccentColors.presets.getOrElse(settings.accentIndex) { AccentColors.presets[0] }
    val invertedTextColor = getInvertedTextColor(currentAccent.primary)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Foundation Surface Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NoteySurfaces.TextPrimary)

        val themeOptions = listOf(
            Triple(ThemeMode.AMOLED_BLACK, "AMOLED Black", NoteyIcons.OledBlack),
            Triple(ThemeMode.ZINC_GREY, "Zinc Grey", NoteyIcons.Moon),
            Triple(ThemeMode.MONET_DYNAMIC, "Monet Dynamic", NoteyIcons.Sun),
            Triple(ThemeMode.DYNAMIC_LIQUID_GLASS, "Liquid Glass", NoteyIcons.Workflow)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            themeOptions.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (mode, label, icon) ->
                        val isSel = settings.themeMode == mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) NoteySurfaces.Surface3 else NoteySurfaces.Surface1,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) MaterialTheme.colorScheme.primary else NoteySurfaces.HairlineBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdate(settings.copy(themeMode = mode)) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSel) MaterialTheme.colorScheme.primary else NoteySurfaces.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = label,
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = if (isSel) NoteySurfaces.TextPrimary else NoteySurfaces.TextSecondary
                                    ),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Text("Dynamic Accent Palette (60%-75% Clamped Luminance)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NoteySurfaces.TextPrimary)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AccentColors.accentSwatches.forEachIndexed { index, color ->
                val isSel = settings.accentIndex == index
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onUpdate(settings.copy(accentIndex = index)) }
                        .border(
                            width = if (isSel) 3.dp else 1.dp,
                            color = if (isSel) Color.White else Color(0x33FFFFFF),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSel) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = getInvertedTextColor(color),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Text("Active Palette Swatch: ${currentAccent.name}", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = currentAccent.primary))

        // Live Elevation & Inversion Preview Card
        Text("Elevation & Dynamic Inversion System Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NoteySurfaces.TextPrimary)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NoteySurfaces.Surface1,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // 3.5dp Accent Left Border
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .fillMaxHeight()
                        .background(currentAccent.primary)
                )

                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Surface 1 (Zinc 950)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = NoteySurfaces.TextPrimary
                        )

                        // Dynamic Inversion Badge: WCAG 2.1 relative luminance
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = currentAccent.primary
                        ) {
                            Text(
                                text = "WCAG 2.1 INVERTED",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = invertedTextColor
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1px Hairline Border: rgba(255, 255, 255, 0.08). The badge text color dynamically inverts to Deep Obsidian (#000000) or Crisp White (#FFFFFF) based on relative luminance Y > 0.42.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NoteySurfaces.TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NoteySurfaces.Surface2,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder)
                        ) {
                            Text(
                                text = "Surface 2",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NoteySurfaces.TextMuted)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NoteySurfaces.Surface3,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder)
                        ) {
                            Text(
                                text = "Surface 3",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NoteySurfaces.TextMuted)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = NoteySurfaces.Surface4,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder)
                        ) {
                            Text(
                                text = "Surface 4",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = NoteySurfaces.TextMuted)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimationSettingsView(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Enable Motion Physics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = NoteySurfaces.TextPrimary)
                Text("Physics-based spring curves across screens and canvas elements", style = MaterialTheme.typography.bodySmall, color = NoteySurfaces.TextSecondary)
            }
            Switch(
                checked = settings.animationsEnabled,
                onCheckedChange = { onUpdate(settings.copy(animationsEnabled = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Reduce Motion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = NoteySurfaces.TextPrimary)
                Text("Instantaneous state changes without transforms", style = MaterialTheme.typography.bodySmall, color = NoteySurfaces.TextSecondary)
            }
            Switch(
                checked = settings.reduceMotion,
                onCheckedChange = { onUpdate(settings.copy(reduceMotion = it)) }
            )
        }

        Text("Animation Physics Timing", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = NoteySurfaces.TextPrimary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                AnimationSpeed.INSTANT,
                AnimationSpeed.FLUID,
                AnimationSpeed.SPRING
            ).forEach { speed ->
                val isSel = settings.animationSpeed == speed
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) NoteySurfaces.Surface3 else NoteySurfaces.Surface1,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSel) MaterialTheme.colorScheme.primary else NoteySurfaces.HairlineBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onUpdate(settings.copy(animationSpeed = speed)) }
                ) {
                    Text(
                        text = speed.label,
                        modifier = Modifier.padding(12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) MaterialTheme.colorScheme.primary else NoteySurfaces.TextSecondary
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun NotesSettingsView(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Continuous Auto-Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = NoteySurfaces.TextPrimary)
                Text("Debounced local persistence to sandbox database", style = MaterialTheme.typography.bodySmall, color = NoteySurfaces.TextSecondary)
            }
            Switch(
                checked = settings.autoSave,
                onCheckedChange = { onUpdate(settings.copy(autoSave = it)) }
            )
        }
    }
}

@Composable
fun CanvasSettingsView(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Radial Dot Grid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = NoteySurfaces.TextPrimary)
                Text("24dp isometric dot grid (#27272A) on OLED canvas", style = MaterialTheme.typography.bodySmall, color = NoteySurfaces.TextSecondary)
            }
            Switch(
                checked = settings.canvasGrid,
                onCheckedChange = { onUpdate(settings.copy(canvasGrid = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Snap to Coordinates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = NoteySurfaces.TextPrimary)
                Text("Snaps nodes to 20dp grid matrix on drag release", style = MaterialTheme.typography.bodySmall, color = NoteySurfaces.TextSecondary)
            }
            Switch(
                checked = settings.canvasSnapToGrid,
                onCheckedChange = { onUpdate(settings.copy(canvasSnapToGrid = it)) }
            )
        }
    }
}

@Composable
fun StorageSettingsView(noteStore: NoteStore, context: Context) {
    val notes = remember { noteStore.loadAll() }
    val totalFiles = notes.sumOf { it.attachments.size }
    val totalBytes = notes.sumOf { it.attachments.sumOf { a -> a.sizeBytes } }
    val sizeMb = "%.2f MB".format(totalBytes / (1024.0 * 1024.0))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NoteySurfaces.Surface1,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Local Sandboxed Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NoteySurfaces.TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Indexed Notes: ${notes.size}", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NoteySurfaces.TextSecondary))
                Text("• Vault Attachments: $totalFiles", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NoteySurfaces.TextSecondary))
                Text("• File Footprint: $sizeMb", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NoteySurfaces.TextSecondary))
            }
        }
    }
}

@Composable
fun PerformanceSettingsView(settings: AppSettings) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NoteySurfaces.Surface1,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Hardware & Runtime Pipeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NoteySurfaces.TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Pure Compose Hardware Acceleration: Active", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NoteySurfaces.TextSecondary))
                Text("• OLED Black Zero-Subpixel Drain: Active", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NoteySurfaces.TextSecondary))
                Text("• Infinite Canvas Matrix Transformation: Active", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NoteySurfaces.TextSecondary))
                Text("• Offline Zero-Network Architecture: Active", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NoteySurfaces.TextSecondary))
            }
        }
    }
}

@Composable
fun DataPrivacySettingsView(
    noteStore: NoteStore,
    onRequestDeleteAll: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NoteySurfaces.Surface1,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Privacy & Local Guarantee", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NoteySurfaces.TextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Notey operates 100% on your device. Your notes, files, canvases, and workflows are never uploaded to any remote server or third-party service.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NoteySurfaces.TextSecondary
                )
            }
        }

        Button(
            onClick = {
                val json = noteStore.exportJson()
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, "Notey Backup")
                    putExtra(Intent.EXTRA_TEXT, json)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Export Notey Backup"))
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export Notes Backup (JSON)")
        }

        OutlinedButton(
            onClick = {
                noteStore.clearTrash()
                Toast.makeText(context, "Trash cleared", Toast.LENGTH_SHORT).show()
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Empty Trash")
        }

        Button(
            onClick = onRequestDeleteAll,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete All Local Data")
        }
    }
}

@Composable
fun AboutSettingsView(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Luxury Industrial Emblem
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(NoteySurfaces.Surface2)
                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                NoteyIcons.Workflow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "Notey",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = NoteySurfaces.TextPrimary
        )

        Text(
            text = "Version ${SettingsStore.APP_VERSION} (Build ${SettingsStore.BUILD_NUMBER})",
            style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = NoteySurfaces.TextMuted)
        )

        Text(
            text = "Luxury Industrial Local-First Workspace",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SettingsStore.GITHUB_REPO_URL))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Source Code", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Developer Attribution (Preserved)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = NoteySurfaces.Surface1,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = SettingsStore.DEVELOPER_ATTRIBUTION,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NoteySurfaces.TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Crafted with Jetpack Compose & Local-First Architecture",
                    style = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp, color = NoteySurfaces.TextSecondary)
                )
            }
        }
    }
}

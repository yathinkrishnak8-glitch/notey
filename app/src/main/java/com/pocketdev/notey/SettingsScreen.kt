package com.pocketdev.notey

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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = when (currentSubpage) {
                        SettingsSubpage.MAIN -> "Settings"
                        SettingsSubpage.PROFILE -> "User Profile"
                        SettingsSubpage.APPEARANCE -> "Appearance"
                        SettingsSubpage.ANIMATION -> "Animation & Interaction"
                        SettingsSubpage.NOTES -> "Notes & Editing"
                        SettingsSubpage.CANVAS -> "Canvas Preferences"
                        SettingsSubpage.STORAGE -> "Files & Storage"
                        SettingsSubpage.PERFORMANCE -> "Performance"
                        SettingsSubpage.DATA -> "Data & Privacy"
                        SettingsSubpage.ABOUT -> "About Notey"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                if (currentSubpage != SettingsSubpage.MAIN) {
                    IconButton(onClick = { currentSubpage = SettingsSubpage.MAIN }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Settings")
                    }
                } else if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Box(modifier = Modifier.fillMaxSize()) {
            when (currentSubpage) {
                SettingsSubpage.MAIN -> {
                    SettingsMainView(
                        profile = currentProfile,
                        onNavigate = { currentSubpage = it }
                    )
                }
                SettingsSubpage.PROFILE -> {
                    ProfileSettingsView(
                        currentProfile = currentProfile,
                        onSaveProfile = { newProfile ->
                            userProfileStore?.saveProfile(newProfile)
                            onProfileChanged?.invoke(newProfile)
                            currentSubpage = SettingsSubpage.MAIN
                            Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                        },
                        onCancel = { currentSubpage = SettingsSubpage.MAIN }
                    )
                }
                SettingsSubpage.APPEARANCE -> {
                    AppearanceSettingsView(settings = settings, onUpdate = ::update)
                }
                SettingsSubpage.ANIMATION -> {
                    AnimationSettingsView(settings = settings, onUpdate = ::update)
                }
                SettingsSubpage.NOTES -> {
                    NotesSettingsView(settings = settings, onUpdate = ::update)
                }
                SettingsSubpage.CANVAS -> {
                    CanvasSettingsView(settings = settings, onUpdate = ::update)
                }
                SettingsSubpage.STORAGE -> {
                    StorageSettingsView(context = context, noteStore = noteStore)
                }
                SettingsSubpage.PERFORMANCE -> {
                    PerformanceSettingsView(settings = settings)
                }
                SettingsSubpage.DATA -> {
                    DataPrivacySettingsView(
                        noteStore = noteStore,
                        onRequestDeleteAll = { showConfirmDeleteAll = true }
                    )
                }
                SettingsSubpage.ABOUT -> {
                    AboutSettingsView(context = context)
                }
            }
        }
    }

    if (showConfirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteAll = false },
            title = { Text("Delete All Data?", color = MaterialTheme.colorScheme.error) },
            text = { Text("This will permanently remove all notes, attachments, and canvases from this device. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        noteStore.clearAllData()
                        showConfirmDeleteAll = false
                        Toast.makeText(context, "All data deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteAll = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsMainView(
    profile: UserProfile?,
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
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(SettingsSubpage.PROFILE) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar Badge
                val avatarBitmap = remember(profile?.avatarUri) {
                    if (profile?.avatarUri != null) {
                        try { BitmapFactory.decodeFile(profile.avatarUri) } catch (_: Exception) { null }
                    } else null
                }

                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap.asImageBitmap(),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = UserProfileValidator.getInitials(profile?.displayName ?: "User"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.displayName?.ifBlank { "Personal Profile" } ?: "Personal Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize your display name & avatar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "›",
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Light
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        SettingsCategoryCard(
            icon = "🎨",
            title = "Appearance",
            subtitle = "Theme mode, accent colors, typography & UI style",
            onClick = { onNavigate(SettingsSubpage.APPEARANCE) }
        )

        SettingsCategoryCard(
            icon = "⚡",
            title = "Animation & Interaction",
            subtitle = "Motion speed, reduce motion, haptic feedback",
            onClick = { onNavigate(SettingsSubpage.ANIMATION) }
        )

        SettingsCategoryCard(
            icon = "📝",
            title = "Notes & Editing",
            subtitle = "Default styling, sorting, and auto-save options",
            onClick = { onNavigate(SettingsSubpage.NOTES) }
        )

        SettingsCategoryCard(
            icon = "🔀",
            title = "Canvas & Workflows",
            subtitle = "Grid visibility, background pattern, connectors",
            onClick = { onNavigate(SettingsSubpage.CANVAS) }
        )

        SettingsCategoryCard(
            icon = "📁",
            title = "Files & Storage",
            subtitle = "Storage stats, cache usage, backup & restore",
            onClick = { onNavigate(SettingsSubpage.STORAGE) }
        )

        SettingsCategoryCard(
            icon = "🚀",
            title = "Performance",
            subtitle = "Resource consumption, motion & rendering options",
            onClick = { onNavigate(SettingsSubpage.PERFORMANCE) }
        )

        SettingsCategoryCard(
            icon = "🛡️",
            title = "Data & Privacy",
            subtitle = "Local storage management, trash, data deletion",
            onClick = { onNavigate(SettingsSubpage.DATA) }
        )

        SettingsCategoryCard(
            icon = "ℹ️",
            title = "About Notey",
            subtitle = "Version, licenses, GitHub source code, attribution",
            onClick = { onNavigate(SettingsSubpage.ABOUT) }
        )
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
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Avatar Preview
        val avatarBitmap = remember(avatarUri) {
            if (avatarUri != null) {
                try { BitmapFactory.decodeFile(avatarUri) } catch (_: Exception) { null }
            } else null
        }

        Box(
            modifier = Modifier
                .size(90.dp)
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
                    text = UserProfileValidator.getInitials(nameInput.ifBlank { "User" }),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { photoPickerLauncher.launch("image/*") }
            ) {
                Text("📷 Choose Photo")
            }
            if (avatarUri != null) {
                TextButton(onClick = { avatarUri = null }) {
                    Text("Remove Photo")
                }
            }
        }

        OutlinedTextField(
            value = nameInput,
            onValueChange = {
                nameInput = it
                errorMessage = UserProfileValidator.validateDisplayName(it)
            },
            label = { Text("Display Name") },
            placeholder = { Text("Enter your preferred name") },
            isError = errorMessage != null,
            supportingText = {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                } else {
                    Text("${nameInput.trim().length} / 40 characters")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    val validation = UserProfileValidator.validateDisplayName(nameInput)
                    if (validation == null) {
                        val updated = (currentProfile ?: UserProfile(displayName = nameInput.trim())).copy(
                            displayName = nameInput.trim(),
                            avatarUri = avatarUri,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSaveProfile(updated)
                    } else {
                        errorMessage = validation
                    }
                },
                enabled = nameInput.trim().isNotEmpty() && errorMessage == null,
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Changes")
            }
        }
    }
}

@Composable
fun SettingsCategoryCard(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "›",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Composable
fun AppearanceSettingsView(
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
        Text("Theme Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(ThemeMode.LIGHT, "Light", "☀️"),
                Triple(ThemeMode.DARK, "Dark", "🌙"),
                Triple(ThemeMode.SYSTEM, "System", "⚙️")
            ).forEach { (mode, label, icon) ->
                val isSel = settings.themeMode == mode
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = if (isSel) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onUpdate(settings.copy(themeMode = mode)) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(icon, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Text("Accent Color Palette", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AccentColors.accentSwatches.forEachIndexed { index, color ->
                val isSel = settings.accentIndex == index
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onUpdate(settings.copy(accentIndex = index)) }
                        .border(
                            width = if (isSel) 3.dp else 1.dp,
                            color = if (isSel) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSel) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Text("UI Shape Style", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Pair(UIStyle.ROUNDED, "Rounded (20dp)"),
                Pair(UIStyle.COMPACT, "Compact (12dp)")
            ).forEach { (style, label) ->
                val isSel = settings.uiStyle == style
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = if (isSel) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onUpdate(settings.copy(uiStyle = style)) }
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Live Preview Card
        Text("Theme Preview", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "✦ Preview: Notey Liquid Glass Card",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This preview responds instantaneously to theme, accent palette, and shape changes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}) { Text("Primary") }
                    FilledTonalButton(onClick = {}) { Text("Tonal") }
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
                Text("Enable Animations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Fluid transitions across screens and cards", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("Reduce Motion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Minimizes motion effects for comfort", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = settings.reduceMotion,
                onCheckedChange = { onUpdate(settings.copy(reduceMotion = it)) }
            )
        }

        Text("Animation Speed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Pair(AnimationSpeed.SLOW, "Slow"),
                Pair(AnimationSpeed.NORMAL, "Normal"),
                Pair(AnimationSpeed.FAST, "Fast")
            ).forEach { (speed, label) ->
                val isSel = settings.animationSpeed == speed
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = if (isSel) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onUpdate(settings.copy(animationSpeed = speed)) }
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Haptic Feedback", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Subtle vibration on button taps and node connections", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = settings.hapticFeedback,
                onCheckedChange = { onUpdate(settings.copy(hapticFeedback = it)) }
            )
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
        Text("Default Sorting", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Pair("updated", "Modified"),
                Pair("created", "Created"),
                Pair("title", "Title")
            ).forEach { (key, label) ->
                val isSel = settings.noteSortBy == key
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = if (isSel) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onUpdate(settings.copy(noteSortBy = key)) }
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto Save", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Automatically save edits as you type", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = settings.autoSave,
                onCheckedChange = { onUpdate(settings.copy(autoSave = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Confirm Delete", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Ask before deleting notes permanently", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = settings.confirmDestructive,
                onCheckedChange = { onUpdate(settings.copy(confirmDestructive = it)) }
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
                Text("Show Canvas Grid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Display subtle alignment dots across workspace", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("Snap to Grid", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Automatically snap nodes during drag", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = settings.canvasSnapToGrid,
                onCheckedChange = { onUpdate(settings.copy(canvasSnapToGrid = it)) }
            )
        }
    }
}

@Composable
fun StorageSettingsView(
    context: Context,
    noteStore: NoteStore
) {
    val attachmentsDir = File(context.filesDir, "attachments")
    val totalSize = attachmentsDir.listFiles()?.sumOf { it.length() } ?: 0L
    val sizeMb = "%.2f MB".format(totalSize / (1024.0 * 1024.0))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Attachments Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Internal Sandboxed Storage: $sizeMb", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "All photos, videos, and files are cloned into app-private internal storage for permanent offline availability.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = {
                val cacheDir = context.cacheDir
                val deleted = cacheDir.deleteRecursively()
                Toast.makeText(context, if (deleted) "Cache cleared" else "Cache already empty", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Temporary Cache")
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
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Performance Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• Jetpack Compose Hardware Acceleration: Enabled", style = MaterialTheme.typography.bodyMedium)
                Text("• Lazy Column Rendering: Active", style = MaterialTheme.typography.bodyMedium)
                Text("• Sandboxed File Resolution: Active", style = MaterialTheme.typography.bodyMedium)
                Text("• Offline Zero-Network Mode: Active", style = MaterialTheme.typography.bodyMedium)
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
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Privacy & Local Guarantee", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Notey operates 100% on your device. Your notes, files, canvases, and workflows are never uploaded to any remote server or third-party service.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export Notes Backup (JSON)")
        }

        OutlinedButton(
            onClick = {
                noteStore.clearTrash()
                Toast.makeText(context, "Trash cleared", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Empty Trash")
        }

        Button(
            onClick = onRequestDeleteAll,
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

        // Purple Feather Logo
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text("🪶", fontSize = 42.sp)
        }

        Text(
            text = "Notey",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Version ${SettingsStore.APP_VERSION} (Build ${SettingsStore.BUILD_NUMBER})",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = "Capture anything. Organize everything. Connect ideas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // View Source Code Button
        Button(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SettingsStore.GITHUB_REPO_URL))
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show()
                }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Source Code", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Developer Attribution
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = SettingsStore.DEVELOPER_ATTRIBUTION,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Crafted with Jetpack Compose & Material Design 3",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

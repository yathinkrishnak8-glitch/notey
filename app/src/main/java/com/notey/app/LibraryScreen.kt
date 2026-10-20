package com.notey.app

import android.content.Context
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    noteStore: NoteStore,
    onOpenNote: ((Long) -> Unit)? = null
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(noteStore.loadAll()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var activePreviewAttachment by remember { mutableStateOf<Attachment?>(null) }

    // Direct Vault File Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val att = FileEngine.copyUriToVault(context, uri)
            if (att != null) {
                val existingVaultNote = notes.firstOrNull { it.folder.equals("Vault", ignoreCase = true) }
                if (existingVaultNote != null) {
                    noteStore.save(existingVaultNote.copy(attachments = existingVaultNote.attachments + att))
                } else {
                    noteStore.save(
                        Note(
                            id = System.currentTimeMillis(),
                            title = "Vault Assets",
                            body = "Repository of files and media imported into your local device vault.",
                            folder = "Vault",
                            attachments = listOf(att)
                        )
                    )
                }
                notes = noteStore.loadAll()
                Toast.makeText(context, "Imported '${att.displayName}' into Vault", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Failed to import file into Vault", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val allAttachments = remember(notes) {
        notes.flatMap { note ->
            note.attachments.map { att -> Pair(att, note) }
        }
    }

    val totalStorageBytes = remember(allAttachments) {
        allAttachments.sumOf { it.first.sizeBytes }
    }
    val totalSizeFormatted = remember(totalStorageBytes) {
        FileEngine.formatFileSize(totalStorageBytes)
    }

    val filterOptions = listOf("All", "Images", "Diagrams", "Docs")

    val filteredList = remember(allAttachments, searchQuery, selectedFilter) {
        allAttachments.filter { (att, _) ->
            val matchesFilter = when (selectedFilter) {
                "Images" -> att.type == AttachmentType.IMAGE
                "Diagrams" -> att.displayName.endsWith(".json", ignoreCase = true) || att.displayName.contains("canvas", ignoreCase = true)
                "Docs" -> att.type == AttachmentType.FILE && !att.displayName.endsWith(".json", ignoreCase = true)
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() || att.displayName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NoteySurfaces.Surface0)
    ) {
        // Vault Header & Storage Management Bar
        Surface(
            color = NoteySurfaces.Surface1,
            border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                NoteyIcons.Vault,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Asset Vault",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = NoteySurfaces.TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${allAttachments.size} assets • $totalSizeFormatted storage",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = NoteySurfaces.TextMuted
                            )
                        )
                    }

                    // "+ Import" action button
                    Button(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Capsule
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NoteySurfaces.Surface2,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = NoteySurfaces.TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = NoteySurfaces.TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif
                            ),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        "Search assets by name or format...",
                                        style = TextStyle(
                                            color = NoteySurfaces.TextMuted,
                                            fontSize = 13.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = NoteySurfaces.TextMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Tabs (All, Images, Diagrams, Docs)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else NoteySurfaces.Surface3,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else NoteySurfaces.HairlineBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else NoteySurfaces.TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Vault Content: High-density 2-Column Grid
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        NoteyIcons.Vault,
                        contentDescription = null,
                        tint = NoteySurfaces.TextMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Vault is Empty",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NoteySurfaces.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Import files directly or attach media to notes",
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            color = NoteySurfaces.TextMuted
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import First File")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.first.id }) { (att, parentNote) ->
                    val file = File(att.filePath)
                    VaultAssetCard(
                        attachment = att,
                        parentNoteTitle = parentNote.title.ifBlank { "Vault Item" },
                        onClick = {
                            if (att.type == AttachmentType.IMAGE || att.type == AttachmentType.VIDEO) {
                                activePreviewAttachment = att
                            } else {
                                // Direct share sheet for documents/files
                                if (file.exists()) {
                                    FileEngine.shareFile(context, file, att.mimeType, att.displayName)
                                }
                            }
                        },
                        onOpenParent = {
                            if (onOpenNote != null) onOpenNote(parentNote.id)
                        },
                        onSaveToDevice = {
                            if (file.exists()) {
                                FileEngine.copyFileToDownloads(context, file, att.mimeType).onSuccess { path ->
                                    Toast.makeText(context, "Saved to $path", Toast.LENGTH_SHORT).show()
                                }.onFailure { err ->
                                    Toast.makeText(context, "Save error: ${err.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onShare = {
                            if (file.exists()) {
                                FileEngine.shareFile(context, file, att.mimeType, att.displayName)
                            }
                        },
                        onDelete = {
                            val updatedAtts = parentNote.attachments.filter { it.id != att.id }
                            try { file.delete() } catch (_: Exception) {}
                            noteStore.save(parentNote.copy(attachments = updatedAtts))
                            notes = noteStore.loadAll()
                            Toast.makeText(context, "Deleted from Vault", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Full-Screen Hardware-Accelerated Media Viewer Dialog
    if (activePreviewAttachment != null) {
        val att = activePreviewAttachment!!
        MediaViewerDialog(
            filePath = att.filePath,
            title = att.displayName,
            onDismiss = { activePreviewAttachment = null }
        )
    }
}

@Composable
fun VaultAssetCard(
    attachment: Attachment,
    parentNoteTitle: String,
    onClick: () -> Unit,
    onOpenParent: () -> Unit,
    onSaveToDevice: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val sizeStr = FileEngine.formatFileSize(attachment.sizeBytes)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = NoteySurfaces.Surface1,
        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Visual Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NoteySurfaces.Surface2),
                contentAlignment = Alignment.Center
            ) {
                if (attachment.type == AttachmentType.IMAGE) {
                    val bmp = remember(attachment.filePath) {
                        try { BitmapFactory.decodeFile(attachment.filePath) } catch (_: Exception) { null }
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = attachment.displayName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(NoteyIcons.Document, contentDescription = null, tint = NoteySurfaces.TextMuted)
                    }
                } else if (attachment.type == AttachmentType.VIDEO) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        NoteyIcons.Document,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File Name
            Text(
                text = attachment.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = NoteySurfaces.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Byte Size in JetBrains Mono style
            Text(
                text = sizeStr,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = NoteySurfaces.TextMuted
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Action Toolbar (Save to Downloads, Share, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Backlink to Parent Note
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onOpenParent),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        NoteyIcons.Paperclip,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = parentNoteTitle,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 10.sp,
                            color = NoteySurfaces.TextSecondary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = onSaveToDevice,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            NoteyIcons.Download,
                            contentDescription = "Save to Device",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            NoteyIcons.Share,
                            contentDescription = "Share",
                            tint = NoteySurfaces.TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

package com.pocketdev.notey

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
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

    val allAttachments = remember(notes) {
        notes.flatMap { note ->
            note.attachments.map { att -> Pair(att, note) }
        }
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
        // Vault Header
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
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                NoteyIcons.Vault,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vault / Asset Manager",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = NoteySurfaces.TextPrimary
                            )
                        }
                        Text(
                            text = "${allAttachments.size} indexed assets in local vault",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = NoteySurfaces.TextMuted
                            )
                        )
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
                                        "Filter by asset name or extension...",
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
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No assets found in vault",
                        style = MaterialTheme.typography.titleMedium,
                        color = NoteySurfaces.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Attached files from notes and canvases appear here",
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = NoteySurfaces.TextMuted
                        )
                    )
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
                    VaultAssetCard(
                        attachment = att,
                        parentNoteTitle = parentNote.title.ifBlank { "Untitled Note" },
                        onClick = { activePreviewAttachment = att },
                        onOpenParent = {
                            if (onOpenNote != null) onOpenNote(parentNote.id)
                        },
                        onDelete = {
                            val updatedAtts = parentNote.attachments.filter { it.id != att.id }
                            try { File(att.filePath).delete() } catch (_: Exception) {}
                            noteStore.save(parentNote.copy(attachments = updatedAtts))
                            notes = noteStore.loadAll()
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
    onDelete: () -> Unit
) {
    val sizeStr = if (attachment.sizeBytes > 1024 * 1024) {
        "%.1f MB".format(attachment.sizeBytes / (1024.0 * 1024.0))
    } else {
        "${attachment.sizeBytes / 1024} KB"
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
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
                    .height(84.dp)
                    .clip(RoundedCornerShape(6.dp))
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
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        NoteyIcons.Document,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
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

            Spacer(modifier = Modifier.height(4.dp))

            // Backlink to Parent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onOpenParent),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    NoteyIcons.Paperclip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = parentNoteTitle,
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 10.sp,
                        color = NoteySurfaces.TextSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

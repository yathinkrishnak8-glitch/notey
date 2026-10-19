package com.pocketdev.notey

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.text.DecimalFormat

// Clamped to 60%–75% luminance
val QuickEditColorSwatches = listOf(
    Color(0xFF38BDF8), // Electric Slate
    Color(0xFF34D399), // Nordic Mint
    Color(0xFFFBBF24), // Solar Amber
    Color(0xFFA78BFA), // Hyper Violet
    Color(0xFFFB7185)  // Crimson Pulse
)

/**
 * Automated Relative Luminance Text Inversion
 * If luminance > 0.42f, returns #000000 (Black), else #FFFFFF (White)
 */
fun getInvertedContrastColor(backgroundColor: Color): Color {
    val luminance = backgroundColor.luminance()
    return if (luminance > 0.42f) Color(0xFF000000) else Color(0xFFFFFFFF)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardQuickEditBottomSheet(
    note: Note,
    noteStore: NoteStore,
    onDismiss: () -> Unit,
    onSaved: (Note) -> Unit
) {
    var title by remember { mutableStateOf(note.title) }
    var excerpt by remember { mutableStateOf(note.body) }
    var tagsText by remember { mutableStateOf(note.folder) }
    var selectedColorIndex by remember { mutableStateOf(note.colorIndex) }
    var attachments by remember { mutableStateOf(note.attachments) }

    val currentColor = GoogleKeepPalette.getAccentColor(selectedColorIndex)
    val invertedTextColor = getInvertedContrastColor(currentColor)

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val copied = noteStore.copyUriToInternalStorage(uri)
            if (copied != null) {
                attachments = attachments + copied
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NoteySurfaces.Surface1,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(NoteySurfaces.Surface4, RoundedCornerShape(2.dp))
            )
        }
    ) {
        BackHandler(enabled = true) {
            onDismiss()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Quick Edit Note",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = NoteySurfaces.TextPrimary
                    )
                    Text(
                        text = "Modify metadata, accent tint, and attachments",
                        style = MonoMetadataStyle,
                        color = NoteySurfaces.TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NoteySurfaces.TextSecondary)
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Note Title") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = currentColor,
                    unfocusedBorderColor = NoteySurfaces.HairlineBorder,
                    focusedTextColor = NoteySurfaces.TextPrimary,
                    unfocusedTextColor = NoteySurfaces.TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Description / Excerpt
            OutlinedTextField(
                value = excerpt,
                onValueChange = { excerpt = it },
                label = { Text("Excerpt / Body Content") },
                minLines = 3,
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = currentColor,
                    unfocusedBorderColor = NoteySurfaces.HairlineBorder,
                    focusedTextColor = NoteySurfaces.TextPrimary,
                    unfocusedTextColor = NoteySurfaces.TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Tags / Folder
            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = { Text("Folder / Tags (e.g. Work, Engineering)") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = currentColor,
                    unfocusedBorderColor = NoteySurfaces.HairlineBorder,
                    focusedTextColor = NoteySurfaces.TextPrimary,
                    unfocusedTextColor = NoteySurfaces.TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Dynamic Color Tint Swatches with Automated Luminance Inversion Preview
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACCENT TINT SWATCH",
                        style = MonoMetadataStyle,
                        fontWeight = FontWeight.Bold,
                        color = NoteySurfaces.TextSecondary
                    )

                    // Live Inverted Contrast Preview Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = currentColor,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "CONTRAST: ${if (currentColor.luminance() > 0.42f) "BLACK" else "WHITE"}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = invertedTextColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickEditColorSwatches.forEachIndexed { idx, swatchColor ->
                        val isSelected = selectedColorIndex == idx
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFFFFFFF) else Color(0x33FFFFFF),
                                    shape = CircleShape
                                )
                                .clickable { selectedColorIndex = idx },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = getInvertedContrastColor(swatchColor),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Attachment Manager (View name/type/byte size, remove 'X', and add file)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ATTACHMENTS (${attachments.size})",
                        style = MonoMetadataStyle,
                        fontWeight = FontWeight.Bold,
                        color = NoteySurfaces.TextSecondary
                    )

                    TextButton(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF38BDF8))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add File", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                if (attachments.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NoteySurfaces.Surface2,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No files attached to this note.",
                            color = NoteySurfaces.TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        attachments.forEachIndexed { attIndex, att ->
                            val f = remember(att.filePath) { File(att.filePath) }
                            val sizeText = remember(att.filePath) {
                                if (f.exists()) {
                                    val b = f.length()
                                    val df = DecimalFormat("#,##0.#")
                                    when {
                                        b >= 1024 * 1024 -> "${df.format(b / (1024.0 * 1024.0))} MB"
                                        b >= 1024 -> "${df.format(b / 1024.0)} KB"
                                        else -> "$b B"
                                    }
                                } else "0 B"
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NoteySurfaces.Surface2,
                                border = androidx.compose.foundation.BorderStroke(1.dp, NoteySurfaces.HairlineBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        NoteyIcons.Paperclip,
                                        contentDescription = null,
                                        tint = currentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = att.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = NoteySurfaces.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${att.type.name} • $sizeText",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = NoteySurfaces.TextSecondary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            attachments = attachments.filterIndexed { i, _ -> i != attIndex }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color(0xFFFB7185),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Save Action Button
            Button(
                onClick = {
                    val updatedNote = note.copy(
                        title = title,
                        body = excerpt,
                        folder = tagsText,
                        colorIndex = selectedColorIndex,
                        attachments = attachments,
                        updatedAt = System.currentTimeMillis()
                    )
                    noteStore.save(updatedNote)
                    onSaved(updatedNote)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = currentColor,
                    contentColor = invertedTextColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

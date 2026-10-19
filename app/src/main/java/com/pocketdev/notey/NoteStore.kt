package com.pocketdev.notey

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class AttachmentType {
    IMAGE, VIDEO, FILE;

    companion object {
        fun fromMimeType(mime: String?): AttachmentType {
            if (mime == null) return FILE
            return when {
                mime.startsWith("image/") -> IMAGE
                mime.startsWith("video/") -> VIDEO
                else -> FILE
            }
        }
    }
}

data class Attachment(
    val id: String = UUID.randomUUID().toString(),
    val filePath: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val type: AttachmentType,
)

data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val isPinned: Boolean = false,
    val colorIndex: Int = 0,
    val attachments: List<Attachment> = emptyList(),
    val updatedAt: Long,
)

/** Lightweight persistence: all notes stored as one JSON array in SharedPreferences. */
class NoteStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("notey_notes", Context.MODE_PRIVATE)

    fun loadAll(): List<Note> {
        val raw = prefs.getString(KEY_NOTES, null) ?: return emptyList()
        val array = JSONArray(raw)
        val notes = ArrayList<Note>(array.length())
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val attachmentsList = mutableListOf<Attachment>()
            if (o.has("attachments")) {
                val atts = o.getJSONArray("attachments")
                for (j in 0 until atts.length()) {
                    val a = atts.getJSONObject(j)
                    val typeStr = a.optString("type", AttachmentType.FILE.name)
                    val type = try {
                        AttachmentType.valueOf(typeStr)
                    } catch (e: Exception) {
                        AttachmentType.FILE
                    }
                    attachmentsList += Attachment(
                        id = a.optString("id", UUID.randomUUID().toString()),
                        filePath = a.getString("filePath"),
                        displayName = a.optString("displayName", "Attachment"),
                        mimeType = a.optString("mimeType", "application/octet-stream"),
                        sizeBytes = a.optLong("sizeBytes", 0L),
                        type = type,
                    )
                }
            }
            notes += Note(
                id = o.getLong("id"),
                title = o.getString("title"),
                body = o.getString("body"),
                isPinned = o.optBoolean("isPinned", false),
                colorIndex = o.optInt("colorIndex", 0),
                attachments = attachmentsList,
                updatedAt = o.getLong("updatedAt"),
            )
        }
        return notes.sortedWith(
            compareByDescending<Note> { it.isPinned }
                .thenByDescending { it.updatedAt }
        )
    }

    fun save(note: Note) {
        val notes = loadAll().filter { it.id != note.id } + note
        writeAll(notes)
    }

    fun delete(id: Long) {
        val toDelete = loadAll().firstOrNull { it.id == id }
        toDelete?.attachments?.forEach { att ->
            try {
                File(att.filePath).delete()
            } catch (_: Exception) {}
        }
        writeAll(loadAll().filter { it.id != id })
    }

    fun togglePin(id: Long) {
        val note = loadAll().firstOrNull { it.id == id } ?: return
        save(note.copy(isPinned = !note.isPinned))
    }

    /** Copy picked URI to internal files directory so it is safely accessible permanently. */
    fun copyUriToInternalStorage(uri: Uri): Attachment? {
        return try {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri) ?: "application/octet-stream"
            var displayName = "file_${System.currentTimeMillis()}"
            var sizeBytes = 0L

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                    if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
                }
            }

            val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
            val extension = displayName.substringAfterLast('.', "")
            val safeName = "${UUID.randomUUID()}" + if (extension.isNotBlank()) ".$extension" else ""
            val destFile = File(attachmentsDir, safeName)

            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (sizeBytes <= 0L) {
                sizeBytes = destFile.length()
            }

            Attachment(
                id = UUID.randomUUID().toString(),
                filePath = destFile.absolutePath,
                displayName = displayName,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                type = AttachmentType.fromMimeType(mimeType)
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun writeAll(notes: List<Note>) {
        val array = JSONArray()
        notes.forEach { n ->
            val attArray = JSONArray()
            n.attachments.forEach { a ->
                attArray.put(
                    JSONObject()
                        .put("id", a.id)
                        .put("filePath", a.filePath)
                        .put("displayName", a.displayName)
                        .put("mimeType", a.mimeType)
                        .put("sizeBytes", a.sizeBytes)
                        .put("type", a.type.name)
                )
            }
            array.put(
                JSONObject()
                    .put("id", n.id)
                    .put("title", n.title)
                    .put("body", n.body)
                    .put("isPinned", n.isPinned)
                    .put("colorIndex", n.colorIndex)
                    .put("attachments", attArray)
                    .put("updatedAt", n.updatedAt)
            )
        }
        prefs.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    private companion object {
        const val KEY_NOTES = "notes"
    }
}

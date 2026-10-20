package com.notey.app

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

data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)

data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val folder: String = "",
    val tags: List<String> = emptyList(),
    val isChecklist: Boolean = false,
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val colorIndex: Int = 0,
    val bgTheme: String = "none",
    val attachments: List<Attachment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    fun parseTasks(): List<TaskItem> {
        if (body.isBlank()) return emptyList()
        return body.lines().mapNotNull { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) null
            else if (trimmed.startsWith("- [x] ", ignoreCase = true) || trimmed.startsWith("[x] ", ignoreCase = true)) {
                val t = trimmed.removePrefix("- [x] ").removePrefix("- [X] ").removePrefix("[x] ").removePrefix("[X] ")
                TaskItem(text = t, isDone = true)
            } else if (trimmed.startsWith("- [ ] ") || trimmed.startsWith("[ ] ")) {
                val t = trimmed.removePrefix("- [ ] ").removePrefix("[ ] ")
                TaskItem(text = t, isDone = false)
            } else {
                TaskItem(text = trimmed, isDone = false)
            }
        }
    }

    fun updateWithTasks(tasks: List<TaskItem>): Note {
        val formatted = tasks.joinToString("\n") { task ->
            val prefix = if (task.isDone) "- [x] " else "- [ ] "
            "$prefix${task.text}"
        }
        return this.copy(body = formatted, isChecklist = true, updatedAt = System.currentTimeMillis())
    }
}

/** Lightweight persistence: all notes stored as one JSON array in SharedPreferences. */
class NoteStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("notey_notes", Context.MODE_PRIVATE)

    fun loadAll(): List<Note> {
        val raw = prefs.getString(KEY_NOTES, null)
        if (raw == null) {
            val initialSamples = createDefaultSampleNotes()
            writeAll(initialSamples)
            return initialSamples
        }
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
                    } catch (_: Exception) {
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

            val tagsList = mutableListOf<String>()
            if (o.has("tags")) {
                val tagsArr = o.getJSONArray("tags")
                for (k in 0 until tagsArr.length()) {
                    tagsList.add(tagsArr.getString(k))
                }
            }

            val updated = o.optLong("updatedAt", System.currentTimeMillis())
            val created = o.optLong("createdAt", updated)

            notes += Note(
                id = o.getLong("id"),
                title = o.optString("title", ""),
                body = o.optString("body", ""),
                isPinned = o.optBoolean("isPinned", false),
                isFavorite = o.optBoolean("isFavorite", false),
                folder = o.optString("folder", ""),
                tags = tagsList,
                isChecklist = o.optBoolean("isChecklist", false),
                isArchived = o.optBoolean("isArchived", false),
                isTrashed = o.optBoolean("isTrashed", false),
                colorIndex = o.optInt("colorIndex", 0),
                bgTheme = o.optString("bgTheme", "none"),
                attachments = attachmentsList,
                createdAt = created,
                updatedAt = updated,
            )
        }
        return notes
    }

    private fun createDefaultSampleNotes(): List<Note> {
        val now = System.currentTimeMillis()
        val groceryTasks = listOf(
            "- [ ] Tomatoes",
            "- [ ] Fresh garlic",
            "- [ ] Chicken breasts",
            "- [ ] Red onions & sweet potatoes",
            "- [ ] Lettuce & cucumbers",
            "- [x] Fresh parsley"
        ).joinToString("\n")

        val groceryNote = Note(
            id = 101L,
            title = "Grocery List",
            body = groceryTasks,
            isPinned = true,
            isChecklist = true,
            colorIndex = 1, // Coral / Peach
            bgTheme = "grocery",
            createdAt = now - 7200000L,
            updatedAt = now - 1800000L
        )

        val gardenNote = Note(
            id = 102L,
            title = "Garden care reminders",
            body = "Remember to water Sarah's garden and feed their cat while they're away next week.\n\nKeep the indoor orchids out of direct sunlight.",
            isPinned = false,
            isChecklist = false,
            colorIndex = 4, // Mint
            bgTheme = "floral",
            createdAt = now - 86400000L,
            updatedAt = now - 3600000L
        )

        val tripNote = Note(
            id = 103L,
            title = "Japan trip itinerary",
            body = "- [ ] Visit Asakusa Sensoji Temple\n- [ ] Hop on the train to Harajuku\n- [ ] Shibuya sky observation deck\n- [x] Book pocket Wi-Fi router",
            isPinned = true,
            isChecklist = true,
            colorIndex = 6, // Fog / Light Blue
            bgTheme = "travel",
            createdAt = now - 172800000L,
            updatedAt = now - 5400000L
        )

        val physicalNote = Note(
            id = 104L,
            title = "Prep for Annual Physical",
            body = "Goal for visit: Discuss knee comfort after morning runs and get annual lab work done.\n\nQuestions:\n- What are the best stretching exercises for flexibility?\n- Could dietary changes improve afternoon energy levels?",
            isPinned = false,
            isChecklist = false,
            colorIndex = 3, // Sand / Yellow
            bgTheme = "none",
            createdAt = now - 259200000L,
            updatedAt = now - 10800000L
        )

        return listOf(groceryNote, gardenNote, tripNote, physicalNote)
    }

    fun getActiveNotes(): List<Note> {
        return loadAll().filter { !it.isTrashed && !it.isArchived }
            .sortedWith(
                compareByDescending<Note> { it.isPinned }
                    .thenByDescending { it.updatedAt }
            )
    }

    fun getFolders(): List<String> {
        return loadAll()
            .map { it.folder.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun getAllTags(): List<String> {
        return loadAll()
            .flatMap { it.tags }
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun save(note: Note) {
        val notes = loadAll().filter { it.id != note.id } + note
        writeAll(notes)
    }

    fun moveToTrash(id: Long) {
        val note = loadAll().firstOrNull { it.id == id } ?: return
        save(note.copy(isTrashed = true, isPinned = false))
    }

    fun restoreFromTrash(id: Long) {
        val note = loadAll().firstOrNull { it.id == id } ?: return
        save(note.copy(isTrashed = false))
    }

    fun permanentlyDelete(id: Long) {
        val toDelete = loadAll().firstOrNull { it.id == id }
        toDelete?.attachments?.forEach { att ->
            try {
                File(att.filePath).delete()
            } catch (_: Exception) {}
        }
        writeAll(loadAll().filter { it.id != id })
    }

    fun clearTrash() {
        val trashed = loadAll().filter { it.isTrashed }
        trashed.forEach { note ->
            note.attachments.forEach { att ->
                try { File(att.filePath).delete() } catch (_: Exception) {}
            }
        }
        writeAll(loadAll().filter { !it.isTrashed })
    }

    fun togglePin(id: Long) {
        val note = loadAll().firstOrNull { it.id == id } ?: return
        save(note.copy(isPinned = !note.isPinned))
    }

    fun toggleFavorite(id: Long) {
        val note = loadAll().firstOrNull { it.id == id } ?: return
        save(note.copy(isFavorite = !note.isFavorite))
    }

    fun toggleTask(id: Long, taskIndex: Int) {
        val note = loadAll().firstOrNull { it.id == id } ?: return
        val tasks = note.parseTasks().toMutableList()
        if (taskIndex in tasks.indices) {
            val current = tasks[taskIndex]
            tasks[taskIndex] = current.copy(isDone = !current.isDone)
            save(note.updateWithTasks(tasks))
        }
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

    fun exportJson(): String {
        return prefs.getString(KEY_NOTES, "[]") ?: "[]"
    }

    fun importJson(jsonString: String): Boolean {
        return try {
            val array = JSONArray(jsonString)
            // Validate parsing
            val imported = ArrayList<Note>(array.length())
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                imported += Note(
                    id = o.getLong("id"),
                    title = o.optString("title", ""),
                    body = o.optString("body", ""),
                    isPinned = o.optBoolean("isPinned", false),
                    isFavorite = o.optBoolean("isFavorite", false),
                    folder = o.optString("folder", ""),
                    colorIndex = o.optInt("colorIndex", 0),
                    bgTheme = o.optString("bgTheme", "none"),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                )
            }
            prefs.edit().putString(KEY_NOTES, jsonString).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun clearAllData() {
        val all = loadAll()
        all.forEach { note ->
            note.attachments.forEach { att ->
                try { File(att.filePath).delete() } catch (_: Exception) {}
            }
        }
        prefs.edit().clear().apply()
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

            val tagsArray = JSONArray()
            n.tags.forEach { tagsArray.put(it) }

            array.put(
                JSONObject()
                    .put("id", n.id)
                    .put("title", n.title)
                    .put("body", n.body)
                    .put("isPinned", n.isPinned)
                    .put("isFavorite", n.isFavorite)
                    .put("folder", n.folder)
                    .put("tags", tagsArray)
                    .put("isChecklist", n.isChecklist)
                    .put("isArchived", n.isArchived)
                    .put("isTrashed", n.isTrashed)
                    .put("colorIndex", n.colorIndex)
                    .put("bgTheme", n.bgTheme)
                    .put("attachments", attArray)
                    .put("createdAt", n.createdAt)
                    .put("updatedAt", n.updatedAt)
            )
        }
        prefs.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    private companion object {
        const val KEY_NOTES = "notes"
    }
}

package com.notey.app

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Notey File & Storage Engine
 * High-performance, offline-first engine for device storage operations,
 * public Downloads exports, FileProvider sharing, and vault asset management.
 */
object FileEngine {

    private const val FILE_PROVIDER_AUTHORITY = "com.notey.app.fileprovider"
    private const val NOTEY_PUBLIC_FOLDER = "Notey"

    /**
     * Get or create the exports working directory inside app storage
     */
    fun getExportDir(context: Context): File {
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Get or create the persistent attachments directory inside app vault
     */
    fun getVaultAttachmentsDir(context: Context): File {
        val dir = File(context.filesDir, "attachments")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Save raw bytes or text directly to the user's public device storage (Downloads/Notey).
     * On Android 10+ (API 29+), uses MediaStore.Downloads with RELATIVE_PATH = Download/Notey.
     * On older versions, writes directly to Environment.DIRECTORY_DOWNLOADS/Notey.
     *
     * Returns Result with the human-readable path string where the file was saved.
     */
    fun saveToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        data: ByteArray
    ): Result<String> {
        return try {
            val sanitizedName = sanitizeFileName(fileName)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizedName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$NOTEY_PUBLIC_FOLDER")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collectionUri, contentValues)
                    ?: return Result.failure(Exception("Failed to allocate MediaStore slot"))

                resolver.openOutputStream(itemUri)?.use { outputStream ->
                    outputStream.write(data)
                    outputStream.flush()
                } ?: return Result.failure(Exception("Unable to open output stream for download"))

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)

                Result.success("Downloads/$NOTEY_PUBLIC_FOLDER/$sanitizedName")
            } else {
                @Suppress("DEPRECATION")
                val publicDownloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val noteyDir = File(publicDownloadDir, NOTEY_PUBLIC_FOLDER).apply { if (!exists()) mkdirs() }
                val targetFile = File(noteyDir, sanitizedName)

                FileOutputStream(targetFile).use { fos ->
                    fos.write(data)
                    fos.flush()
                }
                Result.success(targetFile.absolutePath)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save text content (e.g. Markdown, TXT, HTML, JSON) directly to user's device Downloads
     */
    fun saveTextToDownloads(
        context: Context,
        fileName: String,
        mimeType: String,
        text: String
    ): Result<String> {
        return saveToDownloads(context, fileName, mimeType, text.toByteArray(Charsets.UTF_8))
    }

    /**
     * Save an existing File from app storage into the user's public Downloads/Notey directory
     */
    fun copyFileToDownloads(context: Context, sourceFile: File, mimeType: String): Result<String> {
        return try {
            val bytes = sourceFile.readBytes()
            saveToDownloads(context, sourceFile.name, mimeType, bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Secure Native Share with FileProvider, ClipData, and FLAG_GRANT_READ_URI_PERMISSION.
     * Guaranteed to work across Gmail, WhatsApp, Drive, Telegram, Files, etc.
     */
    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        subject: String = file.name,
        chooserTitle: String = "Share via"
    ): Boolean {
        return try {
            val contentUri: Uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, "Exported from Notey: $subject")
                // Essential for receiving apps on modern Android
                clipData = ClipData.newRawUri(subject, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_LONG).show()
            false
        }
    }

    /**
     * Share plain text via system share sheet
     */
    fun shareText(context: Context, title: String, text: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share Note").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * Import a user-selected file from URI into internal vault storage
     */
    fun copyUriToVault(context: Context, sourceUri: Uri): Attachment? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "attachment_${System.currentTimeMillis()}"
            var fileSize = 0L

            contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                }
            }

            val mime = contentResolver.getType(sourceUri) ?: "application/octet-stream"
            val type = AttachmentType.fromMimeType(mime)

            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "bin"
            val targetFile = File(getVaultAttachmentsDir(context), "${UUID.randomUUID()}_$fileName")

            contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            if (fileSize == 0L) {
                fileSize = targetFile.length()
            }

            Attachment(
                id = UUID.randomUUID().toString(),
                displayName = fileName,
                filePath = targetFile.absolutePath,
                mimeType = mime,
                sizeBytes = fileSize,
                type = type
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Clean and format byte sizes
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val df = DecimalFormat("#,##0.#")
        return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }

    /**
     * Sanitize filename against invalid characters
     */
    fun sanitizeFileName(name: String): String {
        val trimmed = name.trim().ifBlank { "Notey_File" }
        return trimmed.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}

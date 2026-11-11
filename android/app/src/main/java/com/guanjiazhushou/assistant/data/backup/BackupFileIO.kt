package com.guanjiazhushou.assistant.data.backup

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupFileIO {
    const val BACKUP_EXTENSION = ".pmbackup"
    const val BACKUP_MIME_TYPE = "application/octet-stream"

    fun backupStorageDir(context: Context): File =
        File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }

    fun createExportFile(context: Context): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(backupStorageDir(context), "guanjia_backup_$timestamp$BACKUP_EXTENSION")
    }

    /** User-visible relative path under app external storage. */
    fun displaySavePath(context: Context, file: File): String {
        val root = context.getExternalFilesDir(null)?.absolutePath ?: file.parent
        return file.absolutePath.removePrefix("$root/").ifEmpty { file.name }
    }

    fun copyUriToCache(context: Context, uri: Uri): File {
        val dest = File(context.cacheDir, "import_${System.currentTimeMillis()}$BACKUP_EXTENSION")
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("无法读取所选文件")
        input.use { source ->
            dest.outputStream().use { output ->
                source.copyTo(output)
            }
        }
        if (!dest.exists() || dest.length() == 0L) {
            throw IllegalStateException("备份文件为空或无效")
        }
        return dest
    }
}

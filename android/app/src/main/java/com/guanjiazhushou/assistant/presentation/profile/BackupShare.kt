package com.guanjiazhushou.assistant.presentation.profile

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.guanjiazhushou.assistant.data.backup.BackupFileIO
import java.io.File

object BackupShare {
    fun shareBackupFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = BackupFileIO.BACKUP_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "物业管家助手数据备份")
            putExtra(Intent.EXTRA_TEXT, "物业管家助手加密备份文件（${file.name}）")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri("backup", uri)
        }
        context.startActivity(
            Intent.createChooser(intent, "分享备份文件"),
        )
    }
}

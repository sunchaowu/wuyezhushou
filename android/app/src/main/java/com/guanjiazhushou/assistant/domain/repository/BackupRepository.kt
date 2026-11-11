package com.guanjiazhushou.assistant.domain.repository

import com.guanjiazhushou.assistant.domain.model.BackupExportResult
import com.guanjiazhushou.assistant.domain.model.BackupImportResult
import com.guanjiazhushou.assistant.domain.model.BackupInfo
import java.io.File

interface BackupRepository {
    suspend fun exportBackup(password: String, outputFile: File): BackupExportResult

    suspend fun importBackup(password: String, inputFile: File): BackupImportResult

    suspend fun getLastBackupInfo(): BackupInfo?
}

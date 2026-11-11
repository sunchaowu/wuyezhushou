package com.guanjiazhushou.assistant.data.repository

import com.guanjiazhushou.assistant.data.backup.BackupService
import com.guanjiazhushou.assistant.domain.model.BackupExportResult
import com.guanjiazhushou.assistant.domain.model.BackupImportResult
import com.guanjiazhushou.assistant.domain.model.BackupInfo
import com.guanjiazhushou.assistant.domain.repository.BackupRepository
import com.guanjiazhushou.assistant.domain.repository.SettingsRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val backupService: BackupService,
    private val settingsRepository: SettingsRepository,
) : BackupRepository {
    override suspend fun exportBackup(password: String, outputFile: File): BackupExportResult {
        val result = backupService.exportBackup(password, outputFile)
        settingsRepository.setPreference(KEY_LAST_BACKUP_AT, System.currentTimeMillis().toString())
        settingsRepository.setPreference(KEY_LAST_BACKUP_PATH, outputFile.absolutePath)
        settingsRepository.setPreference(KEY_LAST_BACKUP_SIZE, result.fileSizeBytes.toString())
        settingsRepository.setPreference(KEY_LAST_BACKUP_ROOMS, result.roomCount.toString())
        return result
    }

    override suspend fun importBackup(password: String, inputFile: File): BackupImportResult =
        backupService.importBackup(password, inputFile)

    override suspend fun getLastBackupInfo(): BackupInfo? {
        val exportedAt = settingsRepository.getPreference(KEY_LAST_BACKUP_AT)?.toLongOrNull()
            ?: return null
        return BackupInfo(
            exportedAt = exportedAt,
            fileSizeBytes = settingsRepository.getPreference(KEY_LAST_BACKUP_SIZE)?.toLongOrNull() ?: 0L,
            roomCount = settingsRepository.getPreference(KEY_LAST_BACKUP_ROOMS)?.toIntOrNull() ?: 0,
            filePath = settingsRepository.getPreference(KEY_LAST_BACKUP_PATH),
        )
    }

    companion object {
        private const val KEY_LAST_BACKUP_AT = "last_backup_at"
        private const val KEY_LAST_BACKUP_PATH = "last_backup_path"
        private const val KEY_LAST_BACKUP_SIZE = "last_backup_size"
        private const val KEY_LAST_BACKUP_ROOMS = "last_backup_rooms"
    }
}

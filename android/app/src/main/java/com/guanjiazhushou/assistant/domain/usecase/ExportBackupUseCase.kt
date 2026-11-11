package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.exception.BackupPasswordTooShortException
import com.guanjiazhushou.assistant.domain.model.BackupExportResult
import com.guanjiazhushou.assistant.domain.repository.BackupRepository
import java.io.File
import javax.inject.Inject

class ExportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(password: String, outputFile: File): BackupExportResult {
        if (password.length < MIN_PASSWORD_LENGTH) {
            throw BackupPasswordTooShortException()
        }
        return backupRepository.exportBackup(password, outputFile)
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}

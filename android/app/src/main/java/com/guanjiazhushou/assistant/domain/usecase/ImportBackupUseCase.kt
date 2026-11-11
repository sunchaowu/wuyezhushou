package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.model.BackupImportResult
import com.guanjiazhushou.assistant.domain.repository.BackupRepository
import java.io.File
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {
    suspend operator fun invoke(password: String, inputFile: File): BackupImportResult =
        backupRepository.importBackup(password, inputFile)
}

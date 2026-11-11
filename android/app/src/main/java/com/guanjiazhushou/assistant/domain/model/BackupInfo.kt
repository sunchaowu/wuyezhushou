package com.guanjiazhushou.assistant.domain.model

data class BackupInfo(
    val exportedAt: Long,
    val fileSizeBytes: Long,
    val roomCount: Int,
    val filePath: String? = null,
)

data class BackupExportResult(
    val filePath: String,
    val roomCount: Int,
    val fileSizeBytes: Long,
)

data class BackupImportResult(
    val roomCount: Int,
    val importedAt: Long,
)

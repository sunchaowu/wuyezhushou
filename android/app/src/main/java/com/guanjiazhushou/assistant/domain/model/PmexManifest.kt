package com.guanjiazhushou.assistant.domain.model

data class PmexBuildingSummary(
    val name: String,
    val roomCount: Int,
)

data class PmexManifest(
    val formatVersion: Int,
    val packageType: String,
    val appVersion: String,
    val exportedAt: String,
    val senderDeviceName: String,
    val groupName: String?,
    val verifyCode: String,
    val buildings: List<PmexBuildingSummary>,
    val includesCustomIcons: Boolean,
    val checksumSha256: String,
)

data class PmexExportResult(
    val filePath: String,
    val manifest: PmexManifest,
    val fileSizeBytes: Long,
)

data class PmexImportResult(
    val importedBuildingIds: List<Long>,
    val overwritten: Boolean,
    val roomCount: Int,
)

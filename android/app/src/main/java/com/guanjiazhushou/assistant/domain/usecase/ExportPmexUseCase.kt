package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.exception.InvalidVerifyCodeException
import com.guanjiazhushou.assistant.domain.model.PmexExportResult
import com.guanjiazhushou.assistant.domain.repository.ShareRepository
import java.io.File
import javax.inject.Inject

class ExportPmexUseCase @Inject constructor(
    private val shareRepository: ShareRepository,
) {
    suspend operator fun invoke(
        buildingIds: List<Long>,
        senderDeviceName: String,
        verifyCode: String,
        outputFile: File,
        groupName: String? = null,
    ): PmexExportResult {
        if (!VERIFY_CODE_PATTERN.matches(verifyCode)) {
            throw InvalidVerifyCodeException()
        }
        return shareRepository.exportPmex(
            buildingIds = buildingIds,
            senderDeviceName = senderDeviceName,
            groupName = groupName,
            verifyCode = verifyCode,
            outputFile = outputFile,
        )
    }

    companion object {
        private val VERIFY_CODE_PATTERN = Regex("^\\d{6}$")
    }
}

package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.exception.InvalidVerifyCodeException
import com.guanjiazhushou.assistant.domain.model.PmexImportResult
import com.guanjiazhushou.assistant.domain.repository.ShareRepository
import java.io.File
import javax.inject.Inject

class ImportPmexUseCase @Inject constructor(
    private val shareRepository: ShareRepository,
) {
    suspend operator fun invoke(
        inputFile: File,
        verifyCode: String,
        sourceGroupId: Long? = null,
    ): PmexImportResult {
        if (!VERIFY_CODE_PATTERN.matches(verifyCode)) {
            throw InvalidVerifyCodeException()
        }
        return shareRepository.importPmex(
            inputFile = inputFile,
            verifyCode = verifyCode,
            sourceGroupId = sourceGroupId,
        )
    }

    companion object {
        private val VERIFY_CODE_PATTERN = Regex("^\\d{6}$")
    }
}

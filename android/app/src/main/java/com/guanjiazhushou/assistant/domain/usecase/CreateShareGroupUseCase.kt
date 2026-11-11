package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.exception.ActiveShareGroupExistsException
import com.guanjiazhushou.assistant.domain.repository.ShareRepository
import javax.inject.Inject
import kotlin.random.Random

data class CreateShareGroupResult(
    val groupId: Long,
    val verifyCode: String,
)

class CreateShareGroupUseCase @Inject constructor(
    private val shareRepository: ShareRepository,
) {
    suspend operator fun invoke(name: String, hostDeviceName: String): CreateShareGroupResult {
        if (shareRepository.getActiveGroup() != null) {
            throw ActiveShareGroupExistsException()
        }
        val verifyCode = generateVerifyCode()
        val groupId = shareRepository.createGroup(
            name = name,
            hostDeviceName = hostDeviceName,
            verifyCode = verifyCode,
        )
        return CreateShareGroupResult(groupId = groupId, verifyCode = verifyCode)
    }

    private fun generateVerifyCode(): String =
        Random.nextInt(0, 1_000_000).toString().padStart(6, '0')
}

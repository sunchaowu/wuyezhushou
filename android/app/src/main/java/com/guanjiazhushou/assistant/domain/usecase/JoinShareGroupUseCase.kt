package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.exception.ActiveShareGroupExistsException
import com.guanjiazhushou.assistant.domain.exception.InvalidVerifyCodeException
import com.guanjiazhushou.assistant.domain.exception.ShareGroupNotFoundException
import com.guanjiazhushou.assistant.domain.repository.ShareRepository
import javax.inject.Inject

class JoinShareGroupUseCase @Inject constructor(
    private val shareRepository: ShareRepository,
) {
    suspend operator fun invoke(
        groupId: Long,
        verifyCode: String,
        memberDeviceName: String,
    ) {
        if (shareRepository.getActiveGroup() != null) {
            throw ActiveShareGroupExistsException()
        }
        val group = shareRepository.getById(groupId) ?: throw ShareGroupNotFoundException()
        if (group.status != "active" || group.verifyCode != verifyCode) {
            throw InvalidVerifyCodeException()
        }
        shareRepository.joinGroup(groupId, memberDeviceName)
    }
}

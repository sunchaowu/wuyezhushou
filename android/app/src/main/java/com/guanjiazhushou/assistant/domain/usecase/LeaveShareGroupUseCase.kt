package com.guanjiazhushou.assistant.domain.usecase

import com.guanjiazhushou.assistant.domain.exception.NoActiveShareGroupException
import com.guanjiazhushou.assistant.domain.exception.ShareGroupNotFoundException
import com.guanjiazhushou.assistant.domain.repository.ShareRepository
import javax.inject.Inject

class LeaveShareGroupUseCase @Inject constructor(
    private val shareRepository: ShareRepository,
) {
    suspend operator fun invoke(groupId: Long, memberDeviceName: String) {
        val group = shareRepository.getById(groupId) ?: throw ShareGroupNotFoundException()
        if (group.status != "active") {
            throw NoActiveShareGroupException()
        }
        shareRepository.leaveGroup(groupId, memberDeviceName)
    }
}

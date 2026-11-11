package com.guanjiazhushou.assistant.domain.model

data class ShareGroupMember(
    val groupId: Long,
    val memberDeviceName: String,
    val joinedAt: Long,
    val leftAt: Long? = null,
)

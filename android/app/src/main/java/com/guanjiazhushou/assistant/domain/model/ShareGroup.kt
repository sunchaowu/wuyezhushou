package com.guanjiazhushou.assistant.domain.model

data class ShareGroup(
    val id: Long = 0,
    val name: String,
    val hostDeviceName: String,
    val verifyCode: String,
    val status: String = "active",
    val createdAt: Long,
    val dissolvedAt: Long? = null,
)

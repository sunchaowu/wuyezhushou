package com.guanjiazhushou.assistant.domain.model

data class FeatureTag(
    val id: Long = 0,
    val name: String,
    val iconType: IconType,
    val iconKey: String? = null,
    val iconPath: String? = null,
    val isBuiltin: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long,
    /** Descriptive intro for offline listen semantic matching; nullable. */
    val description: String? = null,
)

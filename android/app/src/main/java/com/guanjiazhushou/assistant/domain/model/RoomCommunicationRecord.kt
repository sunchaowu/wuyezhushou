package com.guanjiazhushou.assistant.domain.model

enum class CommunicationRecordType(val key: String) {
    TEXT("text"),
    AUDIO("audio"),
    ;

    companion object {
        fun fromKey(key: String): CommunicationRecordType? =
            entries.find { it.key == key }
    }
}

data class RoomCommunicationRecord(
    val id: Long = 0,
    val roomId: Long,
    val type: CommunicationRecordType,
    val content: String = "",
    val audioPath: String? = null,
    val durationMs: Long = 0,
    val createdAt: Long,
)

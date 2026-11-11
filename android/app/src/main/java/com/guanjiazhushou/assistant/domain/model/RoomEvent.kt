package com.guanjiazhushou.assistant.domain.model

enum class RoomEventType(val key: String, val label: String) {
    REPAIR("repair", "维修报修"),
    COMPLAINT("complaint", "投诉"),
    FOLLOW_UP("follow_up", "催办"),
    OTHER("other", "其他"),
    ;

    companion object {
        fun fromKey(key: String): RoomEventType =
            entries.firstOrNull { it.key == key } ?: OTHER
    }
}

enum class RoomEventStatus(val key: String, val label: String) {
    PENDING("pending", "待处理"),
    IN_PROGRESS("in_progress", "处理中"),
    DONE("done", "已完成"),
    CANCELLED("cancelled", "已取消"),
    ;

    companion object {
        fun fromKey(key: String): RoomEventStatus =
            entries.firstOrNull { it.key == key } ?: PENDING
    }
}

enum class RoomEventSource(val key: String) {
    LISTEN("listen"),
    MANUAL("manual"),
    ;

    companion object {
        fun fromKey(key: String): RoomEventSource =
            entries.firstOrNull { it.key == key } ?: MANUAL
    }
}

data class RoomEvent(
    val id: Long = 0,
    val buildingId: Long,
    val roomId: Long,
    val type: RoomEventType,
    val summary: String,
    val rawText: String? = null,
    val status: RoomEventStatus = RoomEventStatus.PENDING,
    val source: RoomEventSource = RoomEventSource.MANUAL,
    val reminderAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
)

package com.guanjiazhushou.assistant.domain.model

enum class RoomListFilter(val title: String) {
    TOTAL("总户数"),
    OCCUPIED("入住户"),
    ARREARS("欠费户"),
    COLLECTED("已收缴户"),
    RENTED("已租户"),
}

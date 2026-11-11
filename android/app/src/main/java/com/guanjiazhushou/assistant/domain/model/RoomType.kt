package com.guanjiazhushou.assistant.domain.model

enum class RoomType(val key: String, val label: String) {
    RESIDENTIAL("residential", "住宅"),
    SHOP("shop", "商铺");

    companion object {
        fun fromKey(key: String): RoomType? = entries.find { it.key == key }
    }
}

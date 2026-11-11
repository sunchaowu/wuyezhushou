package com.guanjiazhushou.assistant.domain.model

enum class SourceType(val key: String) {
    LOCAL("local"),
    IMPORTED("imported");

    companion object {
        fun fromKey(key: String): SourceType? = entries.find { it.key == key }
    }
}

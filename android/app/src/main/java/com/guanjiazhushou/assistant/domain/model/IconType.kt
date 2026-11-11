package com.guanjiazhushou.assistant.domain.model

enum class IconType(val key: String) {
    BUILTIN("builtin"),
    EMOJI("emoji"),
    CUSTOM_IMAGE("custom_image");

    companion object {
        fun fromKey(key: String): IconType? = entries.find { it.key == key }
    }
}

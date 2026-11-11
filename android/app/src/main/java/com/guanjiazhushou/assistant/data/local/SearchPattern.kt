package com.guanjiazhushou.assistant.data.local

object SearchPattern {
    fun toLikePattern(query: String): String? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        val escaped = trimmed
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        return "%$escaped%"
    }
}

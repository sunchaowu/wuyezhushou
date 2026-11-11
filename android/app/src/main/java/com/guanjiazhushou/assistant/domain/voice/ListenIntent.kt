package com.guanjiazhushou.assistant.domain.voice

data class ListenIntent(
    val label: String,
    val displayName: String,
    val confidence: Float,
    val shouldAct: Boolean,
) {
    companion object {
        private val DISPLAY = mapOf(
            "payment" to "收费跟进",
            "event" to "事件登记",
            "feature_tag" to "特征标签",
            "navigate_context" to "切房上下文",
            "chitchat" to "闲聊",
            "unclear" to "不明确",
        )

        fun fromLabel(label: String, confidence: Float, shouldAct: Boolean): ListenIntent {
            val normalized = if (DISPLAY.containsKey(label)) label else "unclear"
            return ListenIntent(
                label = normalized,
                displayName = DISPLAY.getValue(normalized),
                confidence = confidence,
                shouldAct = shouldAct,
            )
        }
    }
}

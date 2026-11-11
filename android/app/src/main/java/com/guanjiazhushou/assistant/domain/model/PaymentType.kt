package com.guanjiazhushou.assistant.domain.model

enum class PaymentType(val key: String, val label: String, val unit: String) {
    PREPAID("prepaid", "预存", "月"),
    ARREARS("arrears", "欠费", "月"),
    NORMAL("normal", "正常", "月");

    companion object {
        fun fromKey(key: String): PaymentType? = entries.find { it.key == key }
    }
}

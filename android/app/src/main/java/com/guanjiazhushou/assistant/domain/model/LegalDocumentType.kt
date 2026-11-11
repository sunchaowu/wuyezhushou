package com.guanjiazhushou.assistant.domain.model

enum class LegalDocumentType(val title: String, val rawName: String) {
    APP_INTRO(title = "功能介绍", rawName = "app_intro"),
    PRIVACY_POLICY(title = "用户隐私政策", rawName = "privacy_policy"),
    TERMS_OF_SERVICE(title = "用户服务协议", rawName = "terms_of_service"),
    ;

    companion object {
        fun fromRoute(value: String): LegalDocumentType? =
            entries.find { it.name.equals(value, ignoreCase = true) }
    }
}

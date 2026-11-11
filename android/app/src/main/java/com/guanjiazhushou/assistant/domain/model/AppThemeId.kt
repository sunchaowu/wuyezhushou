package com.guanjiazhushou.assistant.domain.model

enum class AppThemeId(val key: String, val label: String, val description: String) {
    BLUE_BUSINESS("blue_business", "蔚蓝商务", "清爽专业，SaaS 风格"),
    EMERALD_MANAGE("emerald_manage", "翡翠管治", "绿色社区，温暖专业"),
    TWILIGHT_VIOLET("twilight_violet", "暮紫渐变", "现代 Web，精致科技感"),
    WARM_SAND("warm_sand", "暖砂质感", "暖色亲和，户外友好"),
    CUTE_PINK("cute_pink", "可爱粉", "甜美清新，少女心氛围"),
    MACARON("macaron", "马卡龙", "柔和 Pastel，低饱和甜系");

    companion object {
        fun fromKey(key: String): AppThemeId? = entries.find { it.key == key }
    }
}

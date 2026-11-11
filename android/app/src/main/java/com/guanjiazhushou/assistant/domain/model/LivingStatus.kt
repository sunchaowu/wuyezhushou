package com.guanjiazhushou.assistant.domain.model

enum class LivingStatus(val key: String, val label: String, val group: String) {
    SELF_LONG_TERM("self_long_term", "长住", "自住"),
    SELF_VACATION("self_vacation", "度假", "自住"),
    RENT_RENTED("rent_rented", "已租", "出租"),
    RENT_AVAILABLE("rent_available", "待租", "出租"),
    VACANT_DELIVERED("vacant_delivered", "已收楼", "空置"),
    VACANT_UNDELIVERED("vacant_undelivered", "未收楼", "空置");

    companion object {
        fun fromKey(key: String): LivingStatus? = entries.find { it.key == key }

        fun resolveLabel(
            statusKey: String,
            settings: List<LivingStatusSetting> = emptyList(),
        ): String = settings.find { it.statusKey == statusKey }?.label
            ?: fromKey(statusKey)?.label
            ?: statusKey
    }
}

package com.guanjiazhushou.assistant.domain.model

enum class BuildingKind(val key: String, val label: String) {
    RESIDENTIAL("residential", "住宅楼栋"),
    SHOP("shop", "商铺楼栋");

    companion object {
        fun fromKey(key: String): BuildingKind? = entries.find { it.key == key }
    }
}

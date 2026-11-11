package com.guanjiazhushou.assistant.domain.model

data class BuiltinFeatureTagDefinition(
    val name: String,
    val iconKey: String,
    val sortOrder: Int,
)

object BuiltinFeatureTags {
    val definitions: List<BuiltinFeatureTagDefinition> = listOf(
        BuiltinFeatureTagDefinition(name = "老人", iconKey = "elderly", sortOrder = 0),
        BuiltinFeatureTagDefinition(name = "小孩", iconKey = "child", sortOrder = 1),
        BuiltinFeatureTagDefinition(name = "宠物", iconKey = "pet", sortOrder = 2),
        BuiltinFeatureTagDefinition(name = "在售", iconKey = "for_sale", sortOrder = 3),
        BuiltinFeatureTagDefinition(name = "车辆", iconKey = "vehicle", sortOrder = 4),
    )
}

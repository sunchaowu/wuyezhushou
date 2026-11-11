package com.guanjiazhushou.assistant.data.voice.nlu

object BuiltinFeatureTagSynonyms {
    private val byName: Map<String, List<String>> = mapOf(
        "老人" to listOf("老人", "老太太", "老爷子", "长者", "高龄"),
        "小孩" to listOf("小孩", "孩子", "小朋友", "宝宝", "儿童"),
        "宠物" to listOf("宠物", "狗", "猫", "小狗", "小猫", "养狗", "养猫"),
        "在售" to listOf("在售", "卖房", "出售", "挂牌", "要卖"),
        "车辆" to listOf("车辆", "有车", "停车", "车位", "汽车"),
    )

    fun synonymsFor(tagName: String): List<String> = byName[tagName].orEmpty()
}

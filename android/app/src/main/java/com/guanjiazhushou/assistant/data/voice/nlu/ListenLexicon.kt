package com.guanjiazhushou.assistant.data.voice.nlu

object ListenLexicon {
    val paymentWords: Set<String> = setOf(
        "欠费", "欠", "缴费", "交费", "交了", "收到", "收款", "到账", "预存",
        "物业费", "月费", "催缴", "没交", "未交", "已交", "付了", "付钱",
    )

    val repairWords: Set<String> = setOf(
        "漏水", "维修", "报修", "修好", "工程", "水管", "停电", "门坏", "门锁",
        "堵塞", "渗水", "漏电", "需要修", "派人",
    )

    val noiseWords: Set<String> = setOf(
        "很吵", "吵闹", "噪音", "扰民", "楼上吵", "吵死", "太吵",
    )

    val questionMarkers: Set<String> = setOf(
        "吗", "嘛", "是不是", "有没有", "收到了吗", "交了吗",
    )

    val hearsayMarkers: Set<String> = setOf(
        "听说", "据说", "好像",
    )

    val answerYes: Set<String> = setOf(
        "收到了", "交了", "有", "是的", "对", "嗯",
    )

    val answerNo: Set<String> = setOf(
        "没收到", "没有", "没交", "未交", "不是", "不对",
    )

    val polarityRemove: Set<String> = setOf(
        "没有", "没", "不", "取消", "去掉", "以前有",
    )

    /** Business words that together with a room number trigger immediate navigation. */
    val businessWords: Set<String> =
        paymentWords + repairWords + noiseWords + setOf(
            "投诉", "催办", "跟进", "沟通", "业主",
        )
}

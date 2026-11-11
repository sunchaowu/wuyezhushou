package com.guanjiazhushou.assistant.domain.model

enum class OnboardingStep(val stepNumber: Int, val title: String, val description: String) {
    CREATE_BUILDING(
        stepNumber = 1,
        title = "创建楼栋",
        description = "点击此处创建楼栋，开始录入房屋数据",
    ),
    FILL_FORM(
        stepNumber = 2,
        title = "填写楼栋信息",
        description = "填写小区名称、楼栋参数等信息，完成后点击保存",
    ),
    MODIFY_DELETE(
        stepNumber = 3,
        title = "管理楼栋",
        description = "点击楼栋进入房间明细；长按可修改或删除",
    ),
    STATISTICS(
        stepNumber = 4,
        title = "查看统计",
        description = "在统计页查看入住率、收缴率等整体数据",
    ),
    ;

    companion object {
        fun fromStored(value: String?): OnboardingStep? = when (value) {
            "1" -> CREATE_BUILDING
            "2" -> FILL_FORM
            "3" -> MODIFY_DELETE
            "4" -> STATISTICS
            else -> null
        }

        const val totalSteps = 4
    }
}

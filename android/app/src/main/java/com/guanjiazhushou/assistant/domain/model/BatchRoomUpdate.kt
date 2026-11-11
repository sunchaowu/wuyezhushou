package com.guanjiazhushou.assistant.domain.model

enum class BatchFeatureTagMode(val label: String) {
    REPLACE("覆盖"),
    ADD("追加"),
    REMOVE("移除"),
}

data class BatchRoomUpdateParams(
    val updatePayment: Boolean = false,
    val paymentType: PaymentType? = null,
    val paymentMonths: Int? = null,
    val updateLivingStatus: Boolean = false,
    val livingStatusKey: String? = null,
    val updateFeatureTags: Boolean = false,
    val featureTagMode: BatchFeatureTagMode = BatchFeatureTagMode.REPLACE,
    val featureTagIds: List<Long> = emptyList(),
) {
    val hasAnyUpdate: Boolean
        get() = updatePayment || updateLivingStatus || updateFeatureTags

    val isValid: Boolean
        get() = when {
            !hasAnyUpdate -> false
            updatePayment && paymentType == null -> false
            updateLivingStatus && livingStatusKey.isNullOrBlank() -> false
            updateFeatureTags && featureTagIds.isEmpty() -> false
            else -> true
        }
}

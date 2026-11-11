package com.guanjiazhushou.assistant.data.bluetooth

data class BluetoothDeviceInfo(
    val address: String,
    val displayName: String,
)

data class BluetoothGroupJoinResult(
    val accepted: Boolean,
    val groupName: String = "",
    val hostDeviceName: String = "",
    val verifyCode: String = "",
)

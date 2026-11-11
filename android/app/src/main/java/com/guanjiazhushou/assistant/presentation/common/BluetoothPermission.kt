package com.guanjiazhushou.assistant.presentation.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.guanjiazhushou.assistant.util.hasBluetoothPermissions
import com.guanjiazhushou.assistant.util.requiredBluetoothPermissions

@Composable
fun rememberBluetoothPermissionGranted(): Boolean {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(hasBluetoothPermissions(context))
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        granted = results.values.all { it }
    }
    LaunchedEffect(Unit) {
        if (!granted) {
            launcher.launch(requiredBluetoothPermissions())
        }
    }
    return granted
}

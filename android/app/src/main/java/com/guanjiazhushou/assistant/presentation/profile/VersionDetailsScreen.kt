package com.guanjiazhushou.assistant.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.guanjiazhushou.assistant.data.appinfo.AppVersionInfo
import com.guanjiazhushou.assistant.ui.theme.AppThemeExtras

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionDetailsScreen(
    onBack: () -> Unit,
    viewModel: VersionDetailsViewModel = hiltViewModel(),
) {
    val palette = AppThemeExtras.palette
    val state = viewModel.uiState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("版本信息") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "应用版本",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                    )
                    VersionInfoRow("应用名称", state.appInfo.appName)
                    VersionInfoRow(
                        "版本号",
                        "${state.appInfo.versionName} (${state.appInfo.versionCode})",
                    )
                    VersionInfoRow("包名", state.appInfo.packageName)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "系统与构建信息",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                    )
                    VersionInfoRow(
                        "本机 Android",
                        "Android ${state.appInfo.deviceAndroidVersion} (API ${state.appInfo.deviceApiLevel})",
                    )
                    VersionInfoRow("最低支持 API", state.appInfo.minSdk.toString())
                    VersionInfoRow("目标 API", state.appInfo.targetSdk.toString())
                    VersionInfoRow("编译 SDK", state.appInfo.compileSdk.toString())
                    VersionInfoRow("Kotlin 版本", state.appInfo.kotlinVersion)
                }
            }

            Text(
                text = "第三方开源组件与许可",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = state.thirdPartyLicenses,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary,
            )
        }
    }
}

@Composable
private fun VersionInfoRow(
    label: String,
    value: String,
) {
    val palette = AppThemeExtras.palette
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textSecondary,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.textPrimary,
        )
    }
}

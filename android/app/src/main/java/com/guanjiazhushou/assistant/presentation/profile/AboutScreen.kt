package com.guanjiazhushou.assistant.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.guanjiazhushou.assistant.domain.model.LegalDocumentType

private sealed interface AboutAction {
    data class OpenDocument(val documentType: LegalDocumentType) : AboutAction
    data object OpenVersionDetails : AboutAction
}

private data class AboutMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val action: AboutAction,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenDocument: (LegalDocumentType) -> Unit,
    onOpenVersionDetails: () -> Unit,
) {
    val menuItems = listOf(
        AboutMenuItem(
            title = "功能介绍",
            subtitle = "了解物业管家助手的主要能力",
            icon = Icons.Default.Info,
            action = AboutAction.OpenDocument(LegalDocumentType.APP_INTRO),
        ),
        AboutMenuItem(
            title = "版本信息",
            subtitle = "应用版本 · 第三方组件与许可",
            icon = Icons.Default.Verified,
            action = AboutAction.OpenVersionDetails,
        ),
        AboutMenuItem(
            title = "用户隐私政策",
            subtitle = "个人信息收集与权限使用说明",
            icon = Icons.Default.Policy,
            action = AboutAction.OpenDocument(LegalDocumentType.PRIVACY_POLICY),
        ),
        AboutMenuItem(
            title = "用户服务协议",
            subtitle = "服务条款与使用规范",
            icon = Icons.Default.Description,
            action = AboutAction.OpenDocument(LegalDocumentType.TERMS_OF_SERVICE),
        ),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于管家助手") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            items(menuItems) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable {
                            when (val action = item.action) {
                                is AboutAction.OpenDocument -> onOpenDocument(action.documentType)
                                AboutAction.OpenVersionDetails -> onOpenVersionDetails()
                            }
                        },
                ) {
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = { Text(item.subtitle) },
                        leadingContent = {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        trailingContent = {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                        },
                    )
                }
            }
        }
    }
}

package com.guanjiazhushou.assistant.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class ProfileMenuItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateTheme: () -> Unit,
    onNavigateBackup: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateGroup: () -> Unit,
    onNavigateAbout: () -> Unit,
    onNavigateEvents: () -> Unit = {},
) {
    val menuItems = listOf(
        ProfileMenuItem("主题色", "切换 6 套预设配色", Icons.Default.Palette, onNavigateTheme),
        ProfileMenuItem("数据备份", "导出/导入 .pmbackup", Icons.Default.Backup, onNavigateBackup),
        ProfileMenuItem("基础设置", "基础数据 · 用户特征画像", Icons.Default.Settings, onNavigateSettings),
        ProfileMenuItem("业务事件", "报修/投诉事件与本地提醒", Icons.AutoMirrored.Filled.EventNote, onNavigateEvents),
        ProfileMenuItem("临时组网", "组网对齐 · 局域网/蓝牙分享楼栋", Icons.Default.Group, onNavigateGroup),
        ProfileMenuItem("关于管家助手", "功能介绍 · 隐私政策 · 服务条款", Icons.Default.Info, onNavigateAbout),
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的") }) },
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
                        .clickable(onClick = item.onClick),
                ) {
                    ListItem(
                        headlineContent = { Text(item.title) },
                        supportingContent = { Text(item.subtitle) },
                        leadingContent = {
                            Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

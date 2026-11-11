package com.guanjiazhushou.assistant.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanjiazhushou.assistant.data.backup.BackupFileIO
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.onImportUriSelected(uri)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.shareFile.collect { file ->
            BackupShare.shareBackupFile(context, file)
        }
    }

    LaunchedEffect(uiState.requestAppRestart) {
        if (uiState.requestAppRestart) {
            viewModel.consumeAppRestartRequest()
            AppRestart.restart(context)
        }
    }

    if (uiState.showExportPasswordDialog) {
        BackupPasswordDialog(
            title = "设置备份密码",
            description = "密码至少 6 位，用于加密 .pmbackup 文件。请牢记密码，导入时需要使用。",
            password = uiState.exportPassword,
            confirmPassword = uiState.exportPasswordConfirm,
            showConfirmField = true,
            isLoading = uiState.isLoading,
            errorMessage = uiState.dialogError,
            onPasswordChange = viewModel::updateExportPassword,
            onConfirmPasswordChange = viewModel::updateExportPasswordConfirm,
            onDismiss = viewModel::dismissExportDialog,
            onConfirm = viewModel::confirmExport,
            confirmLabel = "导出",
        )
    }

    if (uiState.showExportSuccessDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissExportSuccessDialog,
            title = { Text("导出成功") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("备份文件已保存到本机，即使分享失败也可在以下位置找到：")
                    Text(
                        text = uiState.exportedFileDisplayPath.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${uiState.exportedRoomCount} 户 · ${BackupViewModel.formatFileSize(uiState.exportedFileSizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.shareExportedBackup()
                }) {
                    Text("分享")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissExportSuccessDialog) {
                    Text("完成")
                }
            },
        )
    }

    uiState.pendingImportFile?.let {
        if (uiState.showImportConfirmDialog) {
            AlertDialog(
                onDismissRequest = viewModel::dismissImportConfirm,
                title = { Text("确认导入备份") },
                text = {
                    Text(
                        "导入将覆盖当前全部本地数据（楼栋、房间、沟通记录等），且不可撤销。是否继续？",
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::proceedImportConfirm) {
                        Text("继续")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissImportConfirm) {
                        Text("取消")
                    }
                },
            )
        }
    }

    if (uiState.showImportPasswordDialog) {
        BackupPasswordDialog(
            title = "输入备份密码",
            description = "请输入导出该备份文件时设置的密码。",
            password = uiState.importPassword,
            confirmPassword = "",
            showConfirmField = false,
            isLoading = uiState.isLoading,
            errorMessage = uiState.dialogError,
            onPasswordChange = viewModel::updateImportPassword,
            onConfirmPasswordChange = {},
            onDismiss = viewModel::dismissImportPasswordDialog,
            onConfirm = viewModel::confirmImport,
            confirmLabel = "导入",
        )
    }

    uiState.resultMessage?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearResultMessage,
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::clearResultMessage) {
                    Text("确定")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据备份") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "导出或导入 .pmbackup 加密备份文件，可恢复全部本地数据。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            uiState.lastBackupInfo?.let { info ->
                LastBackupInfoCard(info = info)
            }

            if (uiState.isLoading && !uiState.showExportPasswordDialog && !uiState.showImportPasswordDialog) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "处理中…",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            Button(
                onClick = viewModel::openExportDialog,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            ) {
                Text("导出备份")
            }
            if (uiState.canShareLastBackup) {
                OutlinedButton(
                    onClick = viewModel::shareLastBackup,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading,
                ) {
                    Text("分享最近备份")
                }
            }
            OutlinedButton(
                onClick = {
                    importLauncher.launch(
                        arrayOf(BackupFileIO.BACKUP_MIME_TYPE, "application/*", "*/*"),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
            ) {
                Text("导入备份")
            }
            Text(
                text = "导出后文件先保存在本机 backups 目录，再可选择分享；导入时从文件管理器选择 .pmbackup 文件。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LastBackupInfoCard(info: com.guanjiazhushou.assistant.domain.model.BackupInfo) {
    val context = LocalContext.current
    val formattedTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        .format(Date(info.exportedAt))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = "最近备份", style = MaterialTheme.typography.titleSmall)
        Text(
            text = "时间：$formattedTime",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "大小：${BackupViewModel.formatFileSize(info.fileSizeBytes)} · 户数：${info.roomCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        info.filePath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) {
                Text(
                    text = "路径：${BackupFileIO.displaySavePath(context, file)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BackupPasswordDialog(
    title: String,
    description: String,
    password: String,
    confirmPassword: String,
    showConfirmField: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String,
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("密码") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                )
                if (showConfirmField) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = onConfirmPasswordChange,
                        label = { Text("确认密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    )
                }
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isLoading && password.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("取消")
            }
        },
    )
}

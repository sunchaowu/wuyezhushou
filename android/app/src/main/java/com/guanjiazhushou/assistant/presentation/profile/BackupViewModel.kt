package com.guanjiazhushou.assistant.presentation.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guanjiazhushou.assistant.data.backup.BackupFileIO
import com.guanjiazhushou.assistant.domain.exception.BackupPasswordTooShortException
import com.guanjiazhushou.assistant.domain.exception.InvalidBackupPasswordException
import com.guanjiazhushou.assistant.domain.model.BackupInfo
import com.guanjiazhushou.assistant.domain.repository.BackupRepository
import com.guanjiazhushou.assistant.domain.usecase.ExportBackupUseCase
import com.guanjiazhushou.assistant.domain.usecase.ImportBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BackupUiState(
    val lastBackupInfo: BackupInfo? = null,
    val isLoading: Boolean = false,
    val showExportPasswordDialog: Boolean = false,
    val showImportPasswordDialog: Boolean = false,
    val showImportConfirmDialog: Boolean = false,
    val exportPassword: String = "",
    val exportPasswordConfirm: String = "",
    val importPassword: String = "",
    val pendingImportFile: File? = null,
    val dialogError: String? = null,
    val resultMessage: String? = null,
    val requestAppRestart: Boolean = false,
    val showExportSuccessDialog: Boolean = false,
    val exportedFilePath: String? = null,
    val exportedFileDisplayPath: String? = null,
    val exportedRoomCount: Int = 0,
    val exportedFileSizeBytes: Long = 0L,
    val canShareLastBackup: Boolean = false,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _shareFile = MutableSharedFlow<File>(extraBufferCapacity = 1)
    val shareFile: SharedFlow<File> = _shareFile.asSharedFlow()

    init {
        refreshLastBackupInfo()
    }

    fun refreshLastBackupInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = backupRepository.getLastBackupInfo()
            val canShare = info?.filePath?.let { path ->
                File(path).exists() && File(path).length() > 0L
            } == true
            _uiState.update {
                it.copy(
                    lastBackupInfo = info,
                    canShareLastBackup = canShare,
                )
            }
        }
    }

    fun openExportDialog() {
        _uiState.update {
            it.copy(
                showExportPasswordDialog = true,
                exportPassword = "",
                exportPasswordConfirm = "",
                dialogError = null,
            )
        }
    }

    fun dismissExportDialog() {
        if (_uiState.value.isLoading) return
        _uiState.update {
            it.copy(
                showExportPasswordDialog = false,
                exportPassword = "",
                exportPasswordConfirm = "",
                dialogError = null,
            )
        }
    }

    fun updateExportPassword(value: String) {
        _uiState.update { it.copy(exportPassword = value, dialogError = null) }
    }

    fun updateExportPasswordConfirm(value: String) {
        _uiState.update { it.copy(exportPasswordConfirm = value, dialogError = null) }
    }

    fun confirmExport() {
        val state = _uiState.value
        if (state.exportPassword != state.exportPasswordConfirm) {
            _uiState.update { it.copy(dialogError = "两次输入的密码不一致") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, dialogError = null) }
            try {
                val outputFile = BackupFileIO.createExportFile(appContext)
                val result = exportBackupUseCase(state.exportPassword, outputFile)
                refreshLastBackupInfo()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showExportPasswordDialog = false,
                        exportPassword = "",
                        exportPasswordConfirm = "",
                        showExportSuccessDialog = true,
                        exportedFilePath = outputFile.absolutePath,
                        exportedFileDisplayPath = BackupFileIO.displaySavePath(appContext, outputFile),
                        exportedRoomCount = result.roomCount,
                        exportedFileSizeBytes = result.fileSizeBytes,
                        canShareLastBackup = true,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        dialogError = e.toUserMessage(),
                    )
                }
            }
        }
    }

    fun onImportUriSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, dialogError = null) }
            try {
                val file = BackupFileIO.copyUriToCache(appContext, uri)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showImportConfirmDialog = true,
                        pendingImportFile = file,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        resultMessage = e.toUserMessage(),
                    )
                }
            }
        }
    }

    fun dismissImportConfirm() {
        clearPendingImport()
        _uiState.update { it.copy(showImportConfirmDialog = false) }
    }

    fun proceedImportConfirm() {
        _uiState.update {
            it.copy(
                showImportConfirmDialog = false,
                showImportPasswordDialog = true,
                importPassword = "",
                dialogError = null,
            )
        }
    }

    fun dismissImportPasswordDialog() {
        if (_uiState.value.isLoading) return
        clearPendingImport()
        _uiState.update {
            it.copy(
                showImportPasswordDialog = false,
                importPassword = "",
                dialogError = null,
            )
        }
    }

    fun updateImportPassword(value: String) {
        _uiState.update { it.copy(importPassword = value, dialogError = null) }
    }

    fun confirmImport() {
        val state = _uiState.value
        val importFile = state.pendingImportFile ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, dialogError = null) }
            try {
                val result = importBackupUseCase(state.importPassword, importFile)
                importFile.delete()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showImportPasswordDialog = false,
                        pendingImportFile = null,
                        importPassword = "",
                        resultMessage = "导入成功（${result.roomCount} 户），应用将重启以加载新数据",
                        requestAppRestart = true,
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        dialogError = e.toUserMessage(),
                    )
                }
            }
        }
    }

    fun dismissExportSuccessDialog() {
        _uiState.update { it.copy(showExportSuccessDialog = false) }
    }

    fun shareExportedBackup() {
        val path = _uiState.value.exportedFilePath
            ?: _uiState.value.lastBackupInfo?.filePath
            ?: return
        val file = File(path)
        if (!file.exists()) {
            _uiState.update { it.copy(resultMessage = "备份文件不存在，请重新导出") }
            refreshLastBackupInfo()
            return
        }
        viewModelScope.launch {
            _shareFile.emit(file)
        }
    }

    fun shareLastBackup() {
        shareExportedBackup()
    }

    fun clearResultMessage() {
        _uiState.update { it.copy(resultMessage = null) }
    }

    fun consumeAppRestartRequest() {
        _uiState.update { it.copy(requestAppRestart = false) }
    }

    private fun clearPendingImport() {
        _uiState.value.pendingImportFile?.delete()
        _uiState.update { it.copy(pendingImportFile = null) }
    }

    private fun Exception.toUserMessage(): String = when (this) {
        is BackupPasswordTooShortException -> "备份密码至少 6 位"
        is InvalidBackupPasswordException -> "密码错误或备份文件已损坏"
        else -> message ?: "操作失败"
    }

    companion object {
        fun formatFileSize(bytes: Long): String = when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${bytes / 1024}KB"
            else -> String.format("%.1fMB", bytes / (1024.0 * 1024.0))
        }
    }
}

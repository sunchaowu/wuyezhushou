package com.guanjiazhushou.assistant.presentation.legal

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.guanjiazhushou.assistant.domain.model.LegalDocumentType
import com.guanjiazhushou.assistant.ui.theme.AppThemeExtras

@Composable
fun PrivacyConsentScreen(
    onOpenDocument: (LegalDocumentType) -> Unit,
    onAccept: () -> Unit,
) {
    val palette = AppThemeExtras.palette
    val context = LocalContext.current

    BackHandler(enabled = true) {
        // Block back navigation until user explicitly chooses agree or decline.
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "欢迎使用物业管家助手",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "在使用本应用前，请您仔细阅读并充分理解《用户隐私政策》和《用户服务协议》。我们仅在本机存储您的业务数据，并在您主动操作时申请相关系统权限。",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "权限使用说明",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            PermissionDisclosureCard(
                title = "网络与 Wi-Fi",
                detail = "用于临时组网、局域网发现与楼栋数据分享；不经过公网服务器，不使用相关功能时不主动联网。",
            )
            PermissionDisclosureCard(
                title = "蓝牙",
                detail = "用于临时组网备用通道及与已配对设备互传楼栋数据；仅在您主动使用时申请。",
            )
            PermissionDisclosureCard(
                title = "麦克风",
                detail = "用于房屋沟通记录中的语音录入；仅在您点击录音时使用，文件保存在本机。",
            )
            PermissionDisclosureCard(
                title = "图片与文件",
                detail = "用于沟通附件、加密备份导入导出；仅访问您主动选择的文件或本应用目录。",
            )
            PermissionDisclosureCard(
                title = "位置（低版本系统）",
                detail = "Android 11 及以下进行蓝牙扫描时，系统可能要求位置权限；本应用不采集、不上传 GPS 位置信息。",
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TextButton(onClick = { onOpenDocument(LegalDocumentType.PRIVACY_POLICY) }) {
                    Text(
                        text = "《用户隐私政策》",
                        textDecoration = TextDecoration.Underline,
                        color = palette.primary,
                    )
                }
                TextButton(onClick = { onOpenDocument(LegalDocumentType.TERMS_OF_SERVICE) }) {
                    Text(
                        text = "《用户服务协议》",
                        textDecoration = TextDecoration.Underline,
                        color = palette.primary,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "点击「同意并继续」即表示您已阅读并同意上述协议及权限使用说明。",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("同意并继续")
            }
            OutlinedButton(
                onClick = { (context as? Activity)?.finish() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("不同意并退出")
            }
        }
    }
}

@Composable
private fun PermissionDisclosureCard(
    title: String,
    detail: String,
) {
    val palette = AppThemeExtras.palette
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = palette.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun PrivacyConsentLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

package com.guanjiazhushou.assistant.presentation.voice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ListenStatusBar(
    isListening: Boolean,
    isMuted: Boolean,
    errorMessage: String?,
    liveTranscript: String = "",
    onToggleMute: () -> Unit,
    onOpenTextSim: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isListening) return
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = null,
                    )
                    Text(
                        text = when {
                            errorMessage != null -> "听记异常"
                            isMuted -> "听记已静音"
                            else -> "听记中"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Row {
                    TextButton(onClick = onOpenTextSim) {
                        Text("模拟口述")
                    }
                    IconButton(onClick = onToggleMute) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = if (isMuted) "取消静音" else "静音",
                        )
                    }
                    if (errorMessage != null) {
                        TextButton(onClick = onClearError) {
                            Text("知道了")
                        }
                    }
                }
            }
            if (liveTranscript.isNotBlank() && errorMessage == null && !isMuted) {
                Text(
                    text = liveTranscript,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

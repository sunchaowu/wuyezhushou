package com.guanjiazhushou.assistant.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CollectionProgressBackground(
    progress: Float,
    fillColor: Color,
    trackColor: Color,
    progressBarColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(12.dp)
    val borderColor = progressBarColor.copy(alpha = 0.28f)

    Box(
        modifier = modifier
            .clip(shape)
            .border(width = 1.dp, color = borderColor, shape = shape),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(trackColor))
        if (clampedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(clampedProgress)
                    .background(fillColor),
            )
        }
        content()
        LinearProgressIndicator(
            progress = { clampedProgress },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)),
            color = progressBarColor,
            trackColor = progressBarColor.copy(alpha = 0.18f),
        )
    }
}

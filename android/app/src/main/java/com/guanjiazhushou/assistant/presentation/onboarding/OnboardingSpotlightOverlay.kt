package com.guanjiazhushou.assistant.presentation.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import com.guanjiazhushou.assistant.domain.model.OnboardingStep

@Composable
fun OnboardingAnchor(
    step: OnboardingStep,
    onboardingViewModel: OnboardingViewModel,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            val size = coordinates.size
            onboardingViewModel.registerAnchor(
                step = step,
                bounds = Rect(
                    left = position.x,
                    top = position.y,
                    right = position.x + size.width,
                    bottom = position.y + size.height,
                ),
            )
        },
    ) {
        content()
    }
}

@Composable
fun OnboardingSpotlightOverlay(
    step: OnboardingStep,
    targetBounds: Rect?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (targetBounds != null) {
            Box(modifier = Modifier.weight(1f)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
                ) {
                    val paddingPx = 8.dp.toPx()
                    val cornerPx = 12.dp.toPx()
                    val highlightLeft = targetBounds.left - paddingPx
                    val highlightTop = targetBounds.top - paddingPx
                    val highlightWidth = targetBounds.width + paddingPx * 2
                    val highlightHeight = targetBounds.height + paddingPx * 2

                    drawRect(Color.Black.copy(alpha = 0.62f))
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(highlightLeft, highlightTop),
                        size = Size(highlightWidth, highlightHeight),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        blendMode = BlendMode.Clear,
                    )
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f))
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "新手指引 ${step.stepNumber}/${OnboardingStep.totalSteps}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = step.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onSkip) {
                        Text("跳过")
                    }
                    Button(onClick = onNext) {
                        Text(
                            if (step == OnboardingStep.STATISTICS) "完成" else "下一步",
                        )
                    }
                }
            }
        }
    }
}

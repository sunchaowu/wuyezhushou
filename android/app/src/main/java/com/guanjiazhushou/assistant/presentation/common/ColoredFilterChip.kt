package com.guanjiazhushou.assistant.presentation.common

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColoredFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    color: Color,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val labelColor = if (selected) color else color.copy(alpha = 0.92f)
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = {
            Text(
                text = label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = labelColor,
            )
        },
        leadingIcon = leadingIcon,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = labelColor,
            iconColor = labelColor,
            selectedContainerColor = color.copy(alpha = 0.28f),
            selectedLabelColor = labelColor,
            selectedLeadingIconColor = labelColor,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = enabled,
            selected = selected,
            borderColor = color.copy(alpha = 0.45f),
            selectedBorderColor = color,
        ),
    )
}

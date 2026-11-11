package com.guanjiazhushou.assistant.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Wraps children horizontally and flows to the next line when space runs out.
 * Uses [Layout] instead of [androidx.compose.foundation.layout.FlowRow] to avoid
 * Compose compiler / foundation-layout API version mismatches at runtime.
 */
@Composable
fun ChipFlowRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier,
    ) { measurables, constraints ->
        val hGap = horizontalSpacing.roundToPx()
        val vGap = verticalSpacing.roundToPx()
        val maxWidth = constraints.maxWidth
        val itemConstraints = if (constraints.hasBoundedWidth) {
            Constraints(minWidth = 0, maxWidth = maxWidth)
        } else {
            Constraints()
        }

        val placeables = measurables.map { measurable ->
            measurable.measure(itemConstraints)
        }

        if (placeables.isEmpty()) {
            return@Layout layout(0, 0) {}
        }

        val rows = mutableListOf<MutableList<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var rowWidth = 0

        placeables.forEach { placeable ->
            val itemWidth = placeable.width
            val extraGap = if (currentRow.isEmpty()) 0 else hGap
            if (currentRow.isNotEmpty() && rowWidth + extraGap + itemWidth > maxWidth) {
                rows += currentRow
                currentRow = mutableListOf()
                rowWidth = 0
            }
            if (currentRow.isNotEmpty()) rowWidth += hGap
            currentRow += placeable
            rowWidth += itemWidth
        }
        if (currentRow.isNotEmpty()) rows += currentRow

        val rowHeights = rows.map { row -> row.maxOf { it.height } }
        val totalHeight = rowHeights.sum() + vGap * (rows.size - 1).coerceAtLeast(0)
        val totalWidth = rows.maxOfOrNull { row ->
            row.sumOf { it.width } + hGap * (row.size - 1).coerceAtLeast(0)
        } ?: 0

        val layoutWidth = when {
            constraints.hasBoundedWidth -> maxWidth
            else -> totalWidth
        }
        layout(layoutWidth, totalHeight) {
            var y = 0
            rows.forEachIndexed { index, row ->
                var x = 0
                val rowHeight = rowHeights[index]
                row.forEach { placeable ->
                    placeable.placeRelative(x, y + (rowHeight - placeable.height) / 2)
                    x += placeable.width + hGap
                }
                y += rowHeight + vGap
            }
        }
    }
}

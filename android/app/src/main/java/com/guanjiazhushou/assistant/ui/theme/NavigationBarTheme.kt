package com.guanjiazhushou.assistant.ui.theme

import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

data class ThemedNavigationBarStyle(
    val containerColor: Color,
    val itemColors: NavigationBarItemColors,
)

/**
 * Bottom bar uses a [surface]↔[primaryContainer] blend so it stays in the same hue
 * family as [background]; selected tab uses [primary], unselected uses [textSecondary].
 */
@Composable
fun rememberThemedNavigationBarStyle(): ThemedNavigationBarStyle {
    val palette = AppThemeExtras.palette
    val containerColor = remember(palette.id, palette.surface, palette.primaryContainer) {
        lerp(palette.surface, palette.primaryContainer, 0.42f)
    }
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = palette.primary,
        selectedTextColor = palette.primary,
        unselectedIconColor = palette.textSecondary,
        unselectedTextColor = palette.textSecondary,
        indicatorColor = palette.primaryContainer,
    )
    return ThemedNavigationBarStyle(
        containerColor = containerColor,
        itemColors = itemColors,
    )
}

package com.guanjiazhushou.assistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.guanjiazhushou.assistant.domain.model.AppThemeId

val LocalAppPalette = staticCompositionLocalOf { BlueBusinessPalette }

@Composable
fun PropertyManagerTheme(
    themeId: AppThemeId = AppThemeId.BLUE_BUSINESS,
    palette: AppThemePalette = paletteFor(themeId),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isSystemInDarkTheme()) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.secondary,
            onSecondary = palette.onPrimary,
            tertiary = palette.accent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.textSecondary,
            error = palette.error,
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primaryContainer,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.secondary,
            onSecondary = palette.onPrimary,
            tertiary = palette.accent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceVariant,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.surfaceVariant,
            outlineVariant = palette.background,
            error = palette.error,
        )
    }

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

object AppThemeExtras {
    val palette: AppThemePalette
        @Composable get() = LocalAppPalette.current
}

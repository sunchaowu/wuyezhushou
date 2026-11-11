package com.guanjiazhushou.assistant.ui.theme

import androidx.compose.ui.graphics.Color
import com.guanjiazhushou.assistant.domain.model.AppThemeId
import com.guanjiazhushou.assistant.domain.model.LivingStatus

data class AppThemePalette(
    val id: AppThemeId,
    val name: String,
    val description: String,
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val error: Color = SemanticColors.Error,
    val success: Color = SemanticColors.Success,
    val neutral: Color = SemanticColors.Neutral,
    val textPrimary: Color,
    val textSecondary: Color,
    /** Foreground on [primary] surfaces; defaults to [surface] for saturated primaries. */
    val onPrimary: Color = surface,
    val readonlyBadge: Color,
    /** Filled portion of building card collection-rate progress background. */
    val collectionProgressFill: Color,
    /** Unfilled track of building card collection-rate progress background. */
    val collectionProgressTrack: Color,
    val chartColors: List<Color>,
    val livingColors: Map<LivingStatus, Color>,
    val headerGradient: Pair<Color, Color>? = null,
)

private fun color(hex: Long) = Color(hex)

/**
 * Page background sits on the mono-hue ladder below [primaryContainer] (衬托底衬、降明度).
 * [collectionProgressTrack] stays near [surface] so list cards lift off the page canvas.
 */
val BlueBusinessPalette = AppThemePalette(
    id = AppThemeId.BLUE_BUSINESS,
    name = AppThemeId.BLUE_BUSINESS.label,
    description = AppThemeId.BLUE_BUSINESS.description,
    primary = color(0xFF2563EB),
    primaryContainer = color(0xFFDBEAFE),
    secondary = color(0xFF0EA5E9),
    accent = color(0xFFF59E0B),
    background = color(0xFFE2E8F0),
    surface = color(0xFFFFFFFF),
    surfaceVariant = color(0xFFCBD5E1),
    textPrimary = color(0xFF0F172A),
    textSecondary = color(0xFF64748B),
    readonlyBadge = color(0xFF1D4ED8),
    collectionProgressFill = color(0xFF93C5FD),
    collectionProgressTrack = color(0xFFF8FAFC),
    chartColors = listOf(
        color(0xFF2563EB),
        color(0xFF3B82F6),
        color(0xFF60A5FA),
        color(0xFF0EA5E9),
        color(0xFF38BDF8),
    ),
    livingColors = mapOf(
        LivingStatus.SELF_LONG_TERM to color(0xFF1D4ED8),
        LivingStatus.RENT_RENTED to color(0xFF2563EB),
        LivingStatus.SELF_VACATION to color(0xFF38BDF8),
    ),
)

val EmeraldManagePalette = AppThemePalette(
    id = AppThemeId.EMERALD_MANAGE,
    name = AppThemeId.EMERALD_MANAGE.label,
    description = AppThemeId.EMERALD_MANAGE.description,
    primary = color(0xFF059669),
    primaryContainer = color(0xFFD1FAE5),
    secondary = color(0xFF14B8A6),
    accent = color(0xFFF97316),
    background = color(0xFFD8E5DD),
    surface = color(0xFFFFFFFF),
    surfaceVariant = color(0xFFB5CFC2),
    textPrimary = color(0xFF14532D),
    textSecondary = color(0xFF4B5563),
    readonlyBadge = color(0xFF047857),
    collectionProgressFill = color(0xFFA7F3D0),
    collectionProgressTrack = color(0xFFF0FDF4),
    chartColors = listOf(
        color(0xFF059669),
        color(0xFF10B981),
        color(0xFF14B8A6),
        color(0xFF2DD4BF),
        color(0xFF34D399),
    ),
    livingColors = mapOf(
        LivingStatus.SELF_LONG_TERM to color(0xFF059669),
        LivingStatus.RENT_RENTED to color(0xFF14B8A6),
        LivingStatus.SELF_VACATION to color(0xFF2DD4BF),
    ),
)

val TwilightVioletPalette = AppThemePalette(
    id = AppThemeId.TWILIGHT_VIOLET,
    name = AppThemeId.TWILIGHT_VIOLET.label,
    description = AppThemeId.TWILIGHT_VIOLET.description,
    primary = color(0xFF6366F1),
    primaryContainer = color(0xFFEEF2FF),
    secondary = color(0xFFA855F7),
    accent = color(0xFF22D3EE),
    background = color(0xFFD8D2E8),
    surface = color(0xFFFFFFFF),
    surfaceVariant = color(0xFFC4BBD8),
    textPrimary = color(0xFF1E1B4B),
    textSecondary = color(0xFF6B7280),
    readonlyBadge = color(0xFF4F46E5),
    collectionProgressFill = color(0xFFC7D2FE),
    collectionProgressTrack = color(0xFFF5F3FF),
    chartColors = listOf(
        color(0xFF6366F1),
        color(0xFF818CF8),
        color(0xFFA855F7),
        color(0xFFC084FC),
        color(0xFF22D3EE),
    ),
    livingColors = mapOf(
        LivingStatus.SELF_LONG_TERM to color(0xFF6366F1),
        LivingStatus.RENT_RENTED to color(0xFF8B5CF6),
        LivingStatus.SELF_VACATION to color(0xFF22D3EE),
    ),
    headerGradient = color(0xFF6366F1) to color(0xFF8B5CF6),
)

val WarmSandPalette = AppThemePalette(
    id = AppThemeId.WARM_SAND,
    name = AppThemeId.WARM_SAND.label,
    description = AppThemeId.WARM_SAND.description,
    primary = color(0xFFD97706),
    primaryContainer = color(0xFFFEF3C7),
    secondary = color(0xFFB45309),
    accent = color(0xFF2563EB),
    background = color(0xFFE5DDD2),
    surface = color(0xFFFFFFFF),
    surfaceVariant = color(0xFFD4C4B0),
    textPrimary = color(0xFF292524),
    textSecondary = color(0xFF92400E),
    readonlyBadge = color(0xFF2563EB),
    collectionProgressFill = color(0xFFFDE68A),
    collectionProgressTrack = color(0xFFFFFBF5),
    chartColors = listOf(
        color(0xFFD97706),
        color(0xFFF59E0B),
        color(0xFFFBBF24),
        color(0xFFB45309),
        color(0xFF92400E),
    ),
    livingColors = mapOf(
        LivingStatus.SELF_LONG_TERM to color(0xFF92400E),
        LivingStatus.RENT_RENTED to color(0xFFD97706),
        LivingStatus.SELF_VACATION to color(0xFFFBBF24),
    ),
)

/**
 * Scheme E — 可爱粉
 * 主色玫瑰粉 + 浅粉容器/背景（单色层次）；衬托色玫粉同类色；
 * 强调色天蓝与主色粉形成色环对比（粉↔蓝互补点缀）；顶部同色系渐变。
 */
val CutePinkPalette = AppThemePalette(
    id = AppThemeId.CUTE_PINK,
    name = AppThemeId.CUTE_PINK.label,
    description = AppThemeId.CUTE_PINK.description,
    primary = color(0xFFEC4899),
    primaryContainer = color(0xFFFCE7F3),
    secondary = color(0xFFF472B6),
    accent = color(0xFF38BDF8),
    background = color(0xFFE6D6DC),
    surface = color(0xFFFFFFFF),
    surfaceVariant = color(0xFFD4B8C2),
    textPrimary = color(0xFF831843),
    textSecondary = color(0xFF9D174D),
    readonlyBadge = color(0xFFBE185D),
    collectionProgressFill = color(0xFFFBCFE8),
    collectionProgressTrack = color(0xFFFFF5F8),
    chartColors = listOf(
        color(0xFFBE185D),
        color(0xFFDB2777),
        color(0xFFEC4899),
        color(0xFFF472B6),
        color(0xFFF9A8D4),
    ),
    livingColors = mapOf(
        LivingStatus.SELF_LONG_TERM to color(0xFFBE185D),
        LivingStatus.RENT_RENTED to color(0xFFEC4899),
        LivingStatus.SELF_VACATION to color(0xFFF9A8D4),
    ),
    headerGradient = color(0xFFEC4899) to color(0xFFF472B6),
)

/**
 * Scheme F — 马卡龙
 * Pastel 容器/背景承担 60% 单色层次；[primary] 取同色相中明度色阶（非 Pastel），
 * 保证主色文字与按钮前景对比；[textPrimary]/[textSecondary] 对齐 WCAG AA（正文 ≥4.5:1）。
 */
val MacaronPalette = AppThemePalette(
    id = AppThemeId.MACARON,
    name = AppThemeId.MACARON.label,
    description = AppThemeId.MACARON.description,
    primary = color(0xFF6F5A7F),
    primaryContainer = color(0xFFF3E8F8),
    secondary = color(0xFF6E96B8),
    accent = color(0xFF3D9A78),
    background = color(0xFFE8E2EC),
    surface = color(0xFFFFFFFF),
    surfaceVariant = color(0xFFD4C8D8),
    textPrimary = color(0xFF2E2636),
    textSecondary = color(0xFF544A5C),
    onPrimary = color(0xFFFFFFFF),
    readonlyBadge = color(0xFF6F5A7F),
    collectionProgressFill = color(0xFFE8D5F0),
    collectionProgressTrack = color(0xFFFFFBF8),
    chartColors = listOf(
        color(0xFF6F5A7F),
        color(0xFF8B7399),
        color(0xFFA68FB3),
        color(0xFFC4ABCE),
        color(0xFFDBC8E4),
    ),
    livingColors = mapOf(
        LivingStatus.SELF_LONG_TERM to color(0xFF6F5A7F),
        LivingStatus.RENT_RENTED to color(0xFF8B7399),
        LivingStatus.SELF_VACATION to color(0xFFC4ABCE),
    ),
    headerGradient = color(0xFF9B7CAD) to color(0xFFB8A0C8),
)

val AllThemePalettes = listOf(
    BlueBusinessPalette,
    EmeraldManagePalette,
    TwilightVioletPalette,
    WarmSandPalette,
    CutePinkPalette,
    MacaronPalette,
)

fun paletteFor(themeId: AppThemeId): AppThemePalette =
    AllThemePalettes.firstOrNull { it.id == themeId } ?: BlueBusinessPalette

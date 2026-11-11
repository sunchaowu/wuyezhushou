package com.guanjiazhushou.assistant.presentation.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guanjiazhushou.assistant.domain.model.FeatureTag
import com.guanjiazhushou.assistant.domain.model.IconType

fun builtinFeatureIcon(iconKey: String?): ImageVector? = when (iconKey) {
    "elderly" -> Icons.Filled.Accessible
    "child" -> Icons.Filled.ChildCare
    "pet" -> Icons.Filled.Pets
    "for_sale" -> Icons.Filled.Sell
    "vehicle" -> Icons.Filled.DirectionsCar
    "hospital" -> Icons.Filled.LocalHospital
    "warning" -> Icons.Filled.Warning
    "home" -> Icons.Filled.Home
    else -> null
}

@Composable
fun FeatureTagSmallIcon(
    tag: FeatureTag,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
    tint: Color? = null,
) {
    val iconSize = size * 0.72f
    val iconTint = tint ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
    when (tag.iconType) {
        IconType.CUSTOM_IMAGE -> {
            val bitmap: ImageBitmap? = remember(tag.iconPath) {
                tag.iconPath?.let { path ->
                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = tag.name,
                    modifier = modifier
                        .size(size)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                FeatureTagFallbackLabel(tag = tag, modifier = modifier, size = size, tint = iconTint)
            }
        }
        IconType.EMOJI -> {
            Box(
                modifier = modifier.size(size),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tag.iconKey.orEmpty().ifEmpty { tag.name.take(1) },
                    fontSize = (size.value * 0.62f).sp,
                    color = iconTint,
                    maxLines = 1,
                )
            }
        }
        IconType.BUILTIN -> {
            val imageVector = remember(tag.iconKey) { builtinFeatureIcon(tag.iconKey) }
            if (imageVector != null) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = tag.name,
                    modifier = modifier.size(iconSize),
                    tint = iconTint,
                )
            } else {
                FeatureTagFallbackLabel(tag = tag, modifier = modifier, size = size, tint = iconTint)
            }
        }
    }
}

@Composable
private fun FeatureTagFallbackLabel(
    tag: FeatureTag,
    modifier: Modifier,
    size: Dp,
    tint: Color,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tag.name.take(1),
            fontSize = (size.value * 0.55f).sp,
            color = tint,
            maxLines = 1,
        )
    }
}

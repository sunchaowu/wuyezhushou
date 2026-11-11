package com.guanjiazhushou.assistant.presentation.common

import androidx.compose.ui.graphics.Color
import com.guanjiazhushou.assistant.domain.model.FeatureTag
import com.guanjiazhushou.assistant.domain.model.FeatureTagColorDefaults

fun FeatureTag.displayColor(): Color = Color(FeatureTagColorDefaults.forTag(this).toInt())

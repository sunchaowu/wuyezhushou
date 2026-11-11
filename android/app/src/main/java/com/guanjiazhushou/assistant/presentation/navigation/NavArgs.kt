package com.guanjiazhushou.assistant.presentation.navigation

import androidx.lifecycle.SavedStateHandle

/** Navigation [NavType.LongType] args are stored as [Long], not [String]. */
fun SavedStateHandle.getNavLong(key: String): Long? =
    get<Long>(key) ?: get<String>(key)?.toLongOrNull()

fun SavedStateHandle.getNavString(key: String): String? =
    get<String>(key) ?: get<Any>(key)?.toString()

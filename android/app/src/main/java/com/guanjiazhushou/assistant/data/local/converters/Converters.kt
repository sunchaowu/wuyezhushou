package com.guanjiazhushou.assistant.data.local.converters

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? =
        value?.joinToString(LIST_DELIMITER)

    @TypeConverter
    fun toStringList(value: String?): List<String>? =
        value
            ?.split(LIST_DELIMITER)
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }

    companion object {
        private const val LIST_DELIMITER = ","
    }
}

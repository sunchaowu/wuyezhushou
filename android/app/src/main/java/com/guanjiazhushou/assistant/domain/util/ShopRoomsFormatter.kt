package com.guanjiazhushou.assistant.domain.util

object ShopRoomsFormatter {
    fun parse(shopRooms: String): List<String> =
        shopRooms.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun join(numbers: List<String>): String = numbers.joinToString(",")

    fun append(shopRooms: String, number: String): String {
        val trimmed = number.trim()
        if (trimmed.isEmpty()) return shopRooms
        val existing = parse(shopRooms)
        if (trimmed in existing) return shopRooms
        return join(existing + trimmed)
    }

    fun remove(shopRooms: String, number: String): String {
        val trimmed = number.trim()
        return join(parse(shopRooms).filter { it != trimmed })
    }

    fun replace(shopRooms: String, oldNumber: String, newNumber: String): String {
        val old = oldNumber.trim()
        val new = newNumber.trim()
        if (old.isEmpty() || new.isEmpty()) return shopRooms
        val updated = parse(shopRooms).map { if (it == old) new else it }
        return join(updated.distinct())
    }
}

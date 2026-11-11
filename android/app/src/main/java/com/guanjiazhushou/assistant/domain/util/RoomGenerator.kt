package com.guanjiazhushou.assistant.domain.util

import com.guanjiazhushou.assistant.domain.model.BuildingKind
import com.guanjiazhushou.assistant.domain.model.PaymentType
import com.guanjiazhushou.assistant.domain.model.Room
import com.guanjiazhushou.assistant.domain.model.RoomType

object RoomGenerator {
    fun generateRooms(
        buildingId: Long,
        buildingKind: BuildingKind = BuildingKind.RESIDENTIAL,
        startFloor: Int,
        maxFloor: Int,
        unitsPerFloor: Int,
        refugeFloors: String,
        excludeRooms: String,
        shopRooms: String,
        defaultPaymentType: PaymentType = PaymentType.ARREARS,
        timestamp: Long = System.currentTimeMillis(),
    ): List<Room> = when (buildingKind) {
        BuildingKind.RESIDENTIAL -> generateResidentialRooms(
            buildingId = buildingId,
            startFloor = startFloor,
            maxFloor = maxFloor,
            unitsPerFloor = unitsPerFloor,
            refugeFloors = refugeFloors,
            excludeRooms = excludeRooms,
            shopRooms = shopRooms,
            defaultPaymentType = defaultPaymentType,
            timestamp = timestamp,
        )
        BuildingKind.SHOP -> generateShopBuildingRooms(
            buildingId = buildingId,
            startFloor = startFloor,
            maxFloor = maxFloor,
            unitsPerFloor = unitsPerFloor,
            refugeFloors = refugeFloors,
            excludeRooms = excludeRooms,
            shopRooms = shopRooms,
            defaultPaymentType = defaultPaymentType,
            timestamp = timestamp,
        )
    }

    fun generateDefaultShopRoomNumbers(
        startFloor: Int,
        maxFloor: Int,
        unitsPerFloor: Int,
        refugeFloors: String,
        excludeRooms: String,
    ): String = generateShopFloorRooms(
        buildingId = 0L,
        startFloor = startFloor,
        maxFloor = maxFloor,
        unitsPerFloor = unitsPerFloor,
        refugeFloors = refugeFloors,
        excludeRooms = excludeRooms,
        defaultPaymentType = PaymentType.ARREARS,
        timestamp = 0L,
    ).joinToString(",") { it.roomNumber }

    private fun generateResidentialRooms(
        buildingId: Long,
        startFloor: Int,
        maxFloor: Int,
        unitsPerFloor: Int,
        refugeFloors: String,
        excludeRooms: String,
        shopRooms: String,
        defaultPaymentType: PaymentType,
        timestamp: Long,
    ): List<Room> {
        val refugeSet = parseIntList(refugeFloors)
        val excludeSet = parseExcludeRooms(excludeRooms)
        val rooms = mutableListOf<Room>()

        for (floor in startFloor..maxFloor) {
            if (floor in refugeSet) continue
            for (unit in 1..unitsPerFloor) {
                val roomNumber = String.format("%d%02d", floor, unit)
                if (roomNumber in excludeSet) continue
                rooms.add(
                    Room(
                        buildingId = buildingId,
                        floor = floor,
                        roomNumber = roomNumber,
                        roomType = RoomType.RESIDENTIAL,
                        paymentType = defaultPaymentType,
                        updatedAt = timestamp,
                    ),
                )
            }
        }

        parseCommaSeparated(shopRooms).forEach { shopNumber ->
            rooms.add(
                Room(
                    buildingId = buildingId,
                    floor = 0,
                    roomNumber = shopNumber,
                    roomType = RoomType.SHOP,
                    paymentType = defaultPaymentType,
                    updatedAt = timestamp,
                ),
            )
        }

        return rooms
    }

    private fun generateShopBuildingRooms(
        buildingId: Long,
        startFloor: Int,
        maxFloor: Int,
        unitsPerFloor: Int,
        refugeFloors: String,
        excludeRooms: String,
        shopRooms: String,
        defaultPaymentType: PaymentType,
        timestamp: Long,
    ): List<Room> {
        val autoRooms = generateShopFloorRooms(
            buildingId = buildingId,
            startFloor = startFloor,
            maxFloor = maxFloor,
            unitsPerFloor = unitsPerFloor,
            refugeFloors = refugeFloors,
            excludeRooms = excludeRooms,
            defaultPaymentType = defaultPaymentType,
            timestamp = timestamp,
        )
        val explicitNumbers = parseCommaSeparated(shopRooms)
        if (explicitNumbers.isEmpty()) {
            return autoRooms
        }
        val autoNumbers = autoRooms.map { it.roomNumber }
        if (explicitNumbers == autoNumbers) {
            return autoRooms
        }
        return explicitNumbers.map { roomNumber ->
            Room(
                buildingId = buildingId,
                floor = inferFloorFromRoomNumber(roomNumber),
                roomNumber = roomNumber,
                roomType = RoomType.SHOP,
                paymentType = defaultPaymentType,
                updatedAt = timestamp,
            )
        }
    }

    private fun generateShopFloorRooms(
        buildingId: Long,
        startFloor: Int,
        maxFloor: Int,
        unitsPerFloor: Int,
        refugeFloors: String,
        excludeRooms: String,
        defaultPaymentType: PaymentType,
        timestamp: Long,
    ): List<Room> {
        val refugeSet = parseIntList(refugeFloors)
        val excludeSet = parseExcludeRooms(excludeRooms)
        val rooms = mutableListOf<Room>()

        for (floor in startFloor..maxFloor) {
            if (floor in refugeSet) continue
            for (unit in 1..unitsPerFloor) {
                val unitSuffix = unit * 2 - 1
                val roomNumber = String.format("%d%02d", floor, unitSuffix)
                if (roomNumber in excludeSet) continue
                rooms.add(
                    Room(
                        buildingId = buildingId,
                        floor = floor,
                        roomNumber = roomNumber,
                        roomType = RoomType.SHOP,
                        paymentType = defaultPaymentType,
                        updatedAt = timestamp,
                    ),
                )
            }
        }
        return rooms
    }

    fun inferFloorFromRoomNumber(roomNumber: String): Int {
        val normalized = normalizeExcludeRoom(roomNumber)
        return if (normalized.length >= 3) {
            normalized.dropLast(2).toIntOrNull() ?: 0
        } else {
            0
        }
    }

    fun parseExcludeRooms(excludeRooms: String): Set<String> {
        if (excludeRooms.isBlank()) return emptySet()
        return parseCommaSeparated(excludeRooms)
            .map(::normalizeExcludeRoom)
            .toSet()
    }

    fun normalizeExcludeRoom(token: String): String {
        val dashMatch = EXCLUDE_DASH_PATTERN.matchEntire(token)
        if (dashMatch != null) {
            val floor = dashMatch.groupValues[1]
            val unit = dashMatch.groupValues[2].padStart(2, '0')
            return "$floor$unit"
        }
        return token
    }

    fun parseIntList(value: String): Set<Int> {
        if (value.isBlank()) return emptySet()
        return parseCommaSeparated(value)
            .mapNotNull { it.toIntOrNull() }
            .toSet()
    }

    private fun parseCommaSeparated(value: String): List<String> =
        value.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private val EXCLUDE_DASH_PATTERN = Regex("""^(\d+)-(\d+)$""")
}

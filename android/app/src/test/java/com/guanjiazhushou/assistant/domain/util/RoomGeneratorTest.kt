package com.guanjiazhushou.assistant.domain.util

import com.guanjiazhushou.assistant.domain.model.BuildingKind
import com.guanjiazhushou.assistant.domain.model.RoomType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomGeneratorTest {

    @Test
    fun generatesResidentialRoomsByFloorAndUnit() {
        val rooms = RoomGenerator.generateRooms(
            buildingId = 1L,
            startFloor = 1,
            maxFloor = 3,
            unitsPerFloor = 2,
            refugeFloors = "",
            excludeRooms = "",
            shopRooms = "",
        )
        assertEquals(6, rooms.size)
        assertTrue(rooms.any { it.roomNumber == "101" && it.floor == 1 })
        assertTrue(rooms.any { it.roomNumber == "302" && it.floor == 3 })
        assertTrue(rooms.all { it.roomType == RoomType.RESIDENTIAL })
    }

    @Test
    fun skipsRefugeFloorsAndExcludedRooms() {
        val rooms = RoomGenerator.generateRooms(
            buildingId = 1L,
            startFloor = 1,
            maxFloor = 18,
            unitsPerFloor = 3,
            refugeFloors = "13",
            excludeRooms = "102,18-03",
            shopRooms = "",
        )
        assertFalse(rooms.any { it.floor == 13 })
        assertFalse(rooms.any { it.roomNumber == "102" })
        assertFalse(rooms.any { it.roomNumber == "1803" })
    }

    @Test
    fun addsShopRoomsOnFloorZero() {
        val rooms = RoomGenerator.generateRooms(
            buildingId = 1L,
            startFloor = 1,
            maxFloor = 1,
            unitsPerFloor = 1,
            refugeFloors = "",
            excludeRooms = "",
            shopRooms = "S01,铺-01",
        )
        assertEquals(3, rooms.size)
        assertTrue(rooms.any { it.roomNumber == "S01" && it.floor == 0 && it.roomType == RoomType.SHOP })
    }

    @Test
    fun normalizeExcludeRoomParsesDashFormat() {
        assertEquals("1803", RoomGenerator.normalizeExcludeRoom("18-03"))
    }

    @Test
    fun generatesShopBuildingWithOddUnitNumbers() {
        val rooms = RoomGenerator.generateRooms(
            buildingId = 1L,
            buildingKind = BuildingKind.SHOP,
            startFloor = 1,
            maxFloor = 2,
            unitsPerFloor = 2,
            refugeFloors = "",
            excludeRooms = "",
            shopRooms = "",
        )
        assertEquals(4, rooms.size)
        assertTrue(rooms.all { it.roomType == RoomType.SHOP })
        assertTrue(rooms.any { it.roomNumber == "101" && it.floor == 1 })
        assertTrue(rooms.any { it.roomNumber == "103" && it.floor == 1 })
        assertTrue(rooms.any { it.roomNumber == "201" && it.floor == 2 })
        assertTrue(rooms.any { it.roomNumber == "203" && it.floor == 2 })
    }

    @Test
    fun generateDefaultShopRoomNumbersReturnsCommaSeparatedOddNumbers() {
        val preview = RoomGenerator.generateDefaultShopRoomNumbers(
            startFloor = 1,
            maxFloor = 1,
            unitsPerFloor = 3,
            refugeFloors = "",
            excludeRooms = "",
        )
        assertEquals("101,103,105", preview)
    }

    @Test
    fun inferFloorFromRoomNumberExtractsFloorPrefix() {
        assertEquals(18, RoomGenerator.inferFloorFromRoomNumber("1803"))
        assertEquals(1, RoomGenerator.inferFloorFromRoomNumber("101"))
    }
}

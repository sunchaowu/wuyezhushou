package com.guanjiazhushou.assistant.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ShopRoomsFormatterTest {

    @Test
    fun appendAddsNewNumber() {
        assertEquals("S01,S02", ShopRoomsFormatter.append("S01", "S02"))
    }

    @Test
    fun appendSkipsDuplicate() {
        assertEquals("S01", ShopRoomsFormatter.append("S01", "S01"))
    }

    @Test
    fun removeDeletesNumber() {
        assertEquals("S02", ShopRoomsFormatter.remove("S01,S02", "S01"))
    }

    @Test
    fun replaceUpdatesNumber() {
        assertEquals("S01A,S02", ShopRoomsFormatter.replace("S01,S02", "S01", "S01A"))
    }
}

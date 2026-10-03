package com.seleneworlds.common.grid

import kotlin.test.Test
import kotlin.test.assertEquals

class ChunkWindowTest {
    @Test
    fun `around includes floors below the coordinate`() {
        val windows = ChunkWindow.around(Coordinate(0, 0, 2), 16, 0, 2)

        assertEquals(listOf(0, 1, 2, 3, 4), windows.map { it.z })
    }

    @Test
    fun `around syncs only the current floor at negative z`() {
        val windows = ChunkWindow.around(Coordinate(0, 0, -1), 16, 0, 2)

        assertEquals(listOf(-1), windows.map { it.z })
    }

    @Test
    fun `around preserves floor zero synchronization`() {
        val windows = ChunkWindow.around(Coordinate(0, 0, 0), 16, 0, 2)

        assertEquals(listOf(0, 1, 2), windows.map { it.z })
    }
}

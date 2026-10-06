package com.seleneworlds.common.grid

import kotlin.test.Test
import kotlin.test.assertEquals

class ChunkWindowTest {
    @Test
    fun `around includes floors below the coordinate`() {
        val windows = ChunkWindow.around(Coordinate(0, 0, 2), 16, 0, 2)

        assertEquals(listOf(0, 1, 2, 3, 4), windows.map { it.z })
    }
}

package com.seleneworlds.common.grid

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GridTest {
    private val grid = Grid().apply {
        defineDirection("east", Coordinate(1, 0, 0), 0f)
        defineDirection("northwest", Coordinate(-1, 1, 0), 135f)
        defineDirection("up", Coordinate(0, 0, 1), 0f)
    }

    @Test
    fun `configured neighboring steps are allowed`() {
        assertTrue(grid.isAllowedStep(Coordinate(10, 20, 3), Coordinate(11, 20, 3)))
        assertTrue(grid.isAllowedStep(Coordinate(10, 20, 3), Coordinate(9, 21, 3)))
        assertTrue(grid.isAllowedStep(Coordinate(10, 20, 3), Coordinate(10, 20, 4)))
    }

    @Test
    fun `arbitrary destinations are rejected`() {
        val origin = Coordinate(10, 20, 3)
        assertFalse(grid.isAllowedStep(origin, origin))
        assertFalse(grid.isAllowedStep(origin, Coordinate(12, 20, 3)))
        assertFalse(grid.isAllowedStep(origin, Coordinate(11, 21, 3)))
        assertFalse(grid.isAllowedStep(origin, Coordinate(10, 20, 2)))
    }

    @Test
    fun `integer overflow cannot produce a valid step`() {
        assertFalse(
            grid.isAllowedStep(
                Coordinate(Int.MAX_VALUE, 0, 0),
                Coordinate(Int.MIN_VALUE, 0, 0),
            )
        )
    }
}

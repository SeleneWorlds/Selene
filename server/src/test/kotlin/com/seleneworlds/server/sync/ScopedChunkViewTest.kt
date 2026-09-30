package com.seleneworlds.server.sync

import com.seleneworlds.common.grid.ChunkWindow
import com.seleneworlds.common.grid.Coordinate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScopedChunkViewTest {
    private val window = ChunkWindow(16, 32, 2, 4, 4)

    @Test
    fun `empty view is empty for transfer`() {
        assertTrue(ScopedChunkView(window).isEmptyForTransfer())
    }

    @Test
    fun `interior base tile makes view non-empty for transfer`() {
        val view = ScopedChunkView(window)
        view.baseTiles[view.padding + view.padding * view.paddedWidth] = 42

        assertFalse(view.isEmptyForTransfer())
    }

    @Test
    fun `padding base tile does not make view non-empty for transfer`() {
        val view = ScopedChunkView(window)
        view.baseTiles[0] = 42

        assertTrue(view.isEmptyForTransfer())
    }

    @Test
    fun `additional tile makes view non-empty for transfer`() {
        val view = ScopedChunkView(window)
        view.addAdditionalTile(Coordinate(window.x, window.y, window.z), 42)

        assertFalse(view.isEmptyForTransfer())
    }
}

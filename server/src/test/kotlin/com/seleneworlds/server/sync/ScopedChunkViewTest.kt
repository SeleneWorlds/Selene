package com.seleneworlds.server.sync

import com.seleneworlds.common.grid.ChunkWindow
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.server.maps.layers.SparseMapLayer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.assertEquals
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScopedChunkViewTest {
    private val window = ChunkWindow(16, 32, 2, 4, 4)

    private fun removalLayer(coordinate: Coordinate): SparseMapLayer {
        val tiles = TileRegistry(Json)
        tiles.upsertEntry(Identifier.parse("test:removed"),
            JsonObject(mapOf("visual" to JsonPrimitive("test:visual"))), id = 42)
        return SparseMapLayer("removals").apply { removeTile(coordinate, tiles.get(42)!!) }
    }

    @Test
    fun `base removal ignores additional tiles in other cells including when removing padding`() {
        val other = Coordinate(window.x + 1, window.y, window.z)
        for (coordinate in listOf(
            Coordinate(window.x, window.y, window.z),
            Coordinate(window.x - 1, window.y, window.z)
        )) {
            val view = ScopedChunkView(window)
            val index = coordinate.x - window.x + view.padding + view.padding * view.paddedWidth
            view.baseTiles[index] = 42
            view.addAdditionalTile(other, 99)

            view.apply(removalLayer(coordinate))

            assertEquals(0, view.getBaseTileAt(coordinate))
            assertEquals(emptyList(), view.getAdditionalTilesAt(coordinate))
            assertEquals(listOf(99), view.getAdditionalTilesAt(other))
        }
    }

    @Test
    fun `base removal promotes only the first additional tile in the same cell`() {
        val coordinate = Coordinate(window.x, window.y, window.z)
        val other = Coordinate(window.x + 1, window.y, window.z)
        val view = ScopedChunkView(window)
        view.baseTiles[view.padding + view.padding * view.paddedWidth] = 42
        view.addAdditionalTile(coordinate, 71)
        view.addAdditionalTile(coordinate, 72)
        view.addAdditionalTile(other, 99)

        view.apply(removalLayer(coordinate))

        assertEquals(71, view.getBaseTileAt(coordinate))
        assertEquals(listOf(72), view.getAdditionalTilesAt(coordinate))
        assertEquals(listOf(99), view.getAdditionalTilesAt(other))
    }

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

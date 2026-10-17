package com.seleneworlds.server.maps.layers

import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.grid.GridRegistry
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.tiles.TileDefinition
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.tiles.transitions.TransitionRegistry
import com.seleneworlds.server.data.Registries
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DenseMapLayerTest {
    @Test
    fun `removing and swapping empty cells does not resolve tile zero`() {
        val registries = Registries(
            TileRegistry(Json), TransitionRegistry(Json), EntityRegistry(Json), ComponentRegistry(Json),
            GridRegistry(Json), SoundRegistry(Json), CustomRegistries(Json)
        )
        val layer = DenseMapLayer("base", registries)
        val tile = TileDefinition(Identifier.parse("test:depot")).apply { id = 1 }
        for (coordinate in listOf(Coordinate(263, -308, 0), Coordinate(-416, 60, 0))) {
            assertFalse(layer.removeTile(coordinate, tile))
            assertFalse(layer.swapTile(coordinate, tile, tile))
            assertEquals(0, layer.getTileId(coordinate))
            assertTrue(layer.placeTile(coordinate, tile))
            assertEquals(1, layer.getTileId(coordinate))
            layer.resetTile(coordinate)
            assertFalse(layer.removeTile(coordinate, tile))
            assertFalse(layer.swapTile(coordinate, tile, tile))
        }
    }
}

package com.seleneworlds.server.http

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.grid.GridRegistry
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.tiles.transitions.TransitionRegistry
import kotlinx.serialization.json.Json
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.maps.layers.DenseMapLayer
import com.seleneworlds.server.maps.layers.MapTreeLayer
import com.seleneworlds.server.maps.tree.MapTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorldMapImageTest {
    private val registries = Registries(
        TileRegistry(Json), TransitionRegistry(Json), EntityRegistry(Json), ComponentRegistry(Json),
        GridRegistry(Json), SoundRegistry(Json), CustomRegistries(Json)
    )

    @Test
    fun nestedLayersAndFloorsContributeWorldBounds() {
        val layer = DenseMapLayer("base", registries)
        layer.annotateTile(Coordinate(-64, -64, 2), "marker", emptyMap())
        layer.annotateTile(Coordinate(128, 128, 3), "marker", emptyMap())
        val nested = MapTree(registries).apply { layers.add(layer) }
        val tree = MapTree(registries).apply { layers.add(MapTreeLayer("nested", nested)) }
        val overview = createWorldMapImage(tree, 2)
        assertEquals(-64, overview.x)
        assertEquals(-64, overview.y)
        assertEquals(listOf(2, 3), overview.floors)
        assertEquals(layer.chunks.values.first().size, overview.width)
        assertEquals(overview.width, overview.height)
    }

    @Test
    fun emptyMapHasAValidSinglePixelImage() {
        val overview = createWorldMapImage(MapTree(registries), 3)
        assertEquals(1, overview.width)
        assertEquals(1, overview.height)
        assertEquals(3, overview.z)
        assertTrue(overview.image.startsWith("data:image/png;base64,"))
    }
}

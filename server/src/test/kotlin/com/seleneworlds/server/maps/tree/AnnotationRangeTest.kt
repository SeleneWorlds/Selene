package com.seleneworlds.server.maps.tree

import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.grid.GridRegistry
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.tiles.transitions.TransitionRegistry
import com.seleneworlds.server.cameras.viewer.DefaultViewer
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.maps.layers.DenseMapLayer
import com.seleneworlds.server.maps.layers.MapTreeLayer
import com.seleneworlds.server.maps.layers.SparseMapLayer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnnotationRangeTest {
    private val registries = Registries(
        TileRegistry(Json), TransitionRegistry(Json), EntityRegistry(Json), ComponentRegistry(Json),
        GridRegistry(Json), SoundRegistry(Json), CustomRegistries(Json)
    )

    @Test
    fun `range includes boundaries across negative chunks and excludes other floors`() {
        val layer = DenseMapLayer("base", registries)
        val map = MapTree(registries).apply { layers.add(layer) }
        val boundary = Coordinate(-64, -64, 2)
        layer.annotateTile(boundary, "warp", mapOf("x" to 3))
        layer.annotateTile(Coordinate(-67, -64, 2), "outside", emptyMap())
        layer.annotateTile(Coordinate(-64, -64, 3), "upstairs", emptyMap())
        assertEquals(mapOf(boundary to mapOf("warp" to mapOf("x" to 3))),
            map.getAnnotationsInRange(Coordinate(-65, -65, 2), 1, DefaultViewer))
        assertEquals(1, map.getAnnotationsInRange(boundary, 0, DefaultViewer).size)
        assertFailsWith<IllegalArgumentException> { map.getAnnotationsInRange(boundary, -1, DefaultViewer) }
    }

    @Test
    fun `range merges nested layers with overrides removals and visibility`() {
        val coordinate = Coordinate(1, 2, 0)
        val base = DenseMapLayer("base", registries).apply {
            annotateTile(coordinate, "warp", mapOf("x" to 3))
            annotateTile(coordinate, "trigger", mapOf("script" to "old"))
            annotateTile(Coordinate(2, 2, 0), "removed", emptyMap())
        }
        val nested = MapTree(registries).apply { layers.add(base) }
        val overlay = SparseMapLayer("overlay").apply {
            annotateTile(coordinate, "warp", mapOf("x" to 4))
            annotateTile(coordinate, "trigger", null)
            annotateTile(Coordinate(2, 2, 0), "removed", null)
        }
        val hidden = SparseMapLayer("hidden").apply {
            visibilityTags.clear()
            annotateTile(coordinate, "warp", null)
        }
        val map = MapTree(registries).apply {
            layers.add(MapTreeLayer("nested", nested))
            layers.add(overlay)
            layers.add(hidden)
        }
        assertEquals(mapOf(coordinate to mapOf("warp" to mapOf("x" to 4))),
            map.getAnnotationsInRange(coordinate, 2, DefaultViewer))
        overlay.resetTile(coordinate)
        assertEquals(base.getAnnotations(coordinate), map.getAnnotationsInRange(coordinate, 2, DefaultViewer)[coordinate])
    }
}

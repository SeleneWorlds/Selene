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
import com.seleneworlds.server.maps.layers.SparseMapLayer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

import com.seleneworlds.server.saves.MapTreeFormatBinaryV1
import com.seleneworlds.server.saves.MapTreeFormatJsonV1
import com.seleneworlds.server.saves.MapTreeFormat
import com.seleneworlds.server.saves.SaveManager
import com.seleneworlds.server.saves.SavesApi
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.common.serialization.seleneJson
import com.seleneworlds.common.grid.ChunkWindow
import com.seleneworlds.server.sync.ScopedChunkView
import kotlin.io.path.createTempDirectory
import kotlin.test.assertSame
import kotlin.test.assertIs

class AnnotationOverlayPersistenceTest {
    private val registries = Registries(
        TileRegistry(Json), TransitionRegistry(Json), EntityRegistry(Json), ComponentRegistry(Json),
        GridRegistry(Json), SoundRegistry(Json), CustomRegistries(Json)
    )

    private fun formats(): List<MapTreeFormat> = listOf(
        MapTreeFormatBinaryV1(registries), MapTreeFormatJsonV1(registries, seleneJson)
    )

    private val coordinate = Coordinate(-65, 12, 0)

    private fun baseMap() = MapTree(registries).apply {
        annotateTile(coordinate, "warp", mapOf("x" to 1))
        annotateTile(coordinate, "removed", mapOf("script" to "original"))
        annotateTile(coordinate, "untouched", mapOf("value" to true))
    }

    @Test
    fun `separate overlay survives restarts including tombstones without saving transient edits`() {
        for (format in formats()) {
            val directory = createTempDirectory("annotation-overlay").toFile()
            try {
                val file = directory.resolve("overlay.selenemap")
                val saves = SavesApi(SaveManager(format, seleneJson),
                    ServerConfig(savePath = directory.absolutePath))
                val original = baseMap()
                original.annotateTile(coordinate, "warp", mapOf("x" to 2), "admin")
                original.annotateTile(coordinate, "removed", null, "admin")
                original.annotateTile(coordinate, "new", mapOf("text" to "hello"), "admin")
                original.annotateTile(coordinate, "transient", emptyMap())
                saves.save(original.api.getLayer("admin"), file.name)

                repeat(2) {
                    val overlay = saves.loadMapLayer(file.name)
                    assertIs<SparseMapLayer>(overlay.layer)
                    assertEquals("admin", overlay.getName())
                    val restarted = baseMap()
                    restarted.api.addLayer(overlay)
                    val expected: Map<Coordinate, Map<String, com.seleneworlds.common.serialization.SerializedMap>> = mapOf(coordinate to mapOf(
                        "warp" to mapOf("x" to 2), "untouched" to mapOf("value" to true),
                        "new" to mapOf("text" to "hello")
                    ))
                    assertEquals(expected, restarted.getAllAnnotations(DefaultViewer))
                    val view = ScopedChunkView.create(restarted, DefaultViewer, ChunkWindow.at(coordinate, 1))
                    assertEquals(expected[coordinate], view.getAnnotationsAt(coordinate))
                    // The base remains independently usable and unchanged.
                    assertEquals(mapOf("x" to 1), restarted.baseLayer.let {
                        (it as DenseMapLayer).getAnnotations(coordinate)["warp"]
                    })
                    saves.save(restarted.api.getLayer("admin"), file.name)
                }
            } finally {
                directory.deleteRecursively()
            }
        }
    }

    @Test
    fun `map saves remain maps and empty layer saves retain their name`() {
        for (format in formats()) {
            val directory = createTempDirectory("map-layer-types").toFile()
            try {
                val saves = SavesApi(SaveManager(format, seleneJson),
                    ServerConfig(savePath = directory.absolutePath))
                val map = MapTree(registries)
                val empty = map.api.getLayer("empty")
                saves.save(map.api, "map.selenemap")
                assertEquals(1, saves.loadMapTree("map.selenemap").mapTree.layers.size)
                assertFailsWith<IllegalArgumentException> { saves.loadMapLayer("map.selenemap") }
                saves.save(empty, "empty.selenemap")
                val loaded = saves.loadMapLayer("empty.selenemap")
                assertFailsWith<IllegalArgumentException> { saves.loadMapTree("empty.selenemap") }
                assertEquals("empty", loaded.getName())
                val target = baseMap()
                target.api.addLayer(loaded)
                assertSame(loaded.layer, target.api.getLayer("empty").layer)
                assertFailsWith<IllegalArgumentException> { target.api.addLayer(loaded) }

            } finally {
                directory.deleteRecursively()
            }
        }
    }

    @Test
    fun `both formats round trip dense tiles and sparse tile swaps without flattening`() {
        val tileId = com.seleneworlds.common.data.Identifier.parse("test:tile")
        val replacementId = com.seleneworlds.common.data.Identifier.parse("test:replacement")
        registries.tiles.upsertEntry(tileId, kotlinx.serialization.json.JsonObject(mapOf(
            "visual" to kotlinx.serialization.json.JsonPrimitive("test:visual"))), id = 1)
        registries.tiles.upsertEntry(replacementId, kotlinx.serialization.json.JsonObject(mapOf(
            "visual" to kotlinx.serialization.json.JsonPrimitive("test:visual"))), id = 2)
        val tile = registries.tiles.get(1)!!
        val replacement = registries.tiles.get(2)!!
        for (format in formats()) {
            val directory = createTempDirectory("layer-tiles").toFile()
            try {
                val file = directory.resolve("layer.selenemap")
                val dense = DenseMapLayer("terrain", registries).apply {
                    placeTile(coordinate, tile)
                    annotateTile(coordinate, "name", mapOf("text" to "stone"))
                }
                format.saveLayer(file, com.seleneworlds.server.maps.layers.MapLayerApi(dense, registries))
                val loadedDense = format.loadMapLayer(file)
                assertEquals("terrain", loadedDense.getName())
                assertEquals(1, assertIs<DenseMapLayer>(loadedDense.layer).getTileId(coordinate))
                assertEquals(mapOf("name" to mapOf("text" to "stone")),
                    loadedDense.layer.getAnnotations(coordinate))
                val overlay = SparseMapLayer("edits").apply { swapTile(coordinate, tile, replacement) }
                format.saveLayer(file, com.seleneworlds.server.maps.layers.MapLayerApi(overlay, registries))
                val loadedSparse = assertIs<SparseMapLayer>(format.loadMapLayer(file).layer)
                assertIs<com.seleneworlds.server.maps.layers.SparseTileSwap>(loadedSparse.getOperations(coordinate).single())
                val map = MapTree(registries).apply { addLayer(loadedDense.layer); addLayer(loadedSparse) }
                val view = ScopedChunkView.create(map, DefaultViewer, ChunkWindow.at(coordinate, 1))
                assertEquals(2, view.getBaseTileAt(coordinate))
                loadedSparse.annotateTile(coordinate, "name", null)
                format.saveFullyInline(file, map)
                val loadedTree = format.loadMapTree(file)
                assertEquals(2, loadedTree.layers.size)
                val treeView = ScopedChunkView.create(loadedTree, DefaultViewer, ChunkWindow.at(coordinate, 1))
                assertEquals(2, treeView.getBaseTileAt(coordinate))
                assertEquals(emptyMap(), treeView.getAnnotationsAt(coordinate))
            } finally {
                directory.deleteRecursively()
            }
        }
    }

    @Test
    fun `failed serialization preserves the previous save`() {
        val directory = createTempDirectory("annotation-overlay-failure").toFile()
        try {
            val file = directory.resolve("overlay.selenemap").apply { writeText("previous save") }
            val format = object : com.seleneworlds.server.saves.MapTreeFormat {
                override fun loadMapLayer(file: java.io.File): com.seleneworlds.server.maps.layers.MapLayerApi = error("unused")
                override fun saveLayer(file: java.io.File, layer: com.seleneworlds.server.maps.layers.MapLayerApi) = error("unused")
                override fun loadMapTree(file: java.io.File): MapTree = error("unused")
                override fun saveFullyInline(file: java.io.File, mapTree: MapTree) {
                    file.writeText("incomplete")
                    error("serialization failed")
                }
            }
            assertFailsWith<IllegalStateException> { SaveManager(format, seleneJson).save(file, baseMap()) }
            assertEquals("previous save", file.readText())
            assertEquals(listOf(file), directory.listFiles()!!.toList())
        } finally {
            directory.deleteRecursively()
        }
    }
}

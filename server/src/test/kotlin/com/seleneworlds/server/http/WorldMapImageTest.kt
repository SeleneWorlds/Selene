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
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.tiles.TileDefinition
import java.util.Base64
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.assertFailsWith
import com.seleneworlds.common.grid.ChunkWindow
import com.seleneworlds.server.sync.ScopedChunkView

class WorldMapImageTest {
    private val registries = Registries(
        TileRegistry(Json), TransitionRegistry(Json), EntityRegistry(Json), ComponentRegistry(Json),
        GridRegistry(Json), SoundRegistry(Json), CustomRegistries(Json)
    )

    @Test
    fun imageUsesConfiguredMapColorEvenForImpassableTiles() {
        val identifier = Identifier.parse("test:colored")
        registries.tiles.upsertEntry(identifier, Json.parseToJsonElement(
            """{"visual":"test:visual","mapColor":"#123AbC","impassable":true}"""), id = 1)
        val tile = requireNotNull(registries.tiles.get(identifier))
        val tree = MapTree(registries)
        tree.placeTile(Coordinate(0, 0, 0), tile)
        val overview = createWorldMapImage(tree, 0)
        val png = ImageIO.read(ByteArrayInputStream(Base64.getDecoder().decode(overview.image.substringAfter(','))))
        assertEquals(0xff123abc.toInt(), png.getRGB(-overview.x, -overview.y))
    }

    @Test
    fun tileMapColorDefaultsToWhiteAndRejectsInvalidColors() {
        val definition = Json.decodeFromString<TileDefinition>("""{"visual":"test:visual"}""")
        assertEquals("#ffffff", definition.mapColor)
        for (color in listOf("white", "#123", "#gggggg", "#ffffffff")) {
            assertFailsWith<IllegalArgumentException> { TileDefinition(Identifier.parse("test:visual"), mapColor = color) }
        }
    }

    @Test
    fun tileModeChoosesBaseOrTopAdditionalTile() {
        val coordinate = Coordinate(0, 0, 0)
        val view = ScopedChunkView(ChunkWindow(0, 0, 0, 1, 1))
        view.addAdditionalTile(coordinate, 7)
        view.addAdditionalTile(coordinate, 9)
        assertEquals(0, worldMapTile(view, coordinate, WorldMapTiles.BASE))
        assertEquals(9, worldMapTile(view, coordinate, WorldMapTiles.ALL))
        view.baseTiles[view.paddedWidth + 1] = 3
        assertEquals(3, worldMapTile(view, coordinate, WorldMapTiles.BASE))
        assertEquals(9, worldMapTile(view, coordinate, WorldMapTiles.ALL))
        view.additionalTiles.clear()
        assertEquals(3, worldMapTile(view, coordinate, WorldMapTiles.ALL))
    }

    @Test
    fun tileModeDefaultsToAllAndRejectsUnknownValues() {
        assertEquals(WorldMapTiles.ALL, worldMapTiles(null))
        assertEquals(WorldMapTiles.ALL, worldMapTiles("all"))
        assertEquals(WorldMapTiles.BASE, worldMapTiles("base"))
        assertFailsWith<IllegalArgumentException> { worldMapTiles("invalid") }
    }

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

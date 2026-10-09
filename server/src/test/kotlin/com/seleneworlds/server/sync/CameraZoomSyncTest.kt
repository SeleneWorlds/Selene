package com.seleneworlds.server.sync

import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.tiles.TileDefinition
import com.seleneworlds.server.maps.layers.DenseMapLayer
import com.seleneworlds.common.network.packet.MapChunkPacket
import com.seleneworlds.common.network.packet.UpdateMapTilesPacket
import com.seleneworlds.common.grid.ChunkWindow
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.grid.Grid
import com.seleneworlds.common.grid.GridRegistry
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.packet.RemoveMapChunkPacket
import com.seleneworlds.common.sounds.SoundRegistry
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.common.tiles.TileRegistry
import com.seleneworlds.common.tiles.transitions.TransitionRegistry
import com.seleneworlds.server.collision.CollisionResolver
import com.seleneworlds.server.data.Registries
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.dimensions.DimensionManager
import com.seleneworlds.server.entities.EntityManager
import com.seleneworlds.server.network.NetworkClient
import com.seleneworlds.server.players.PlayerManager
import com.seleneworlds.server.tiles.transitions.TransitionResolver
import com.seleneworlds.server.world.World
import kotlinx.serialization.json.Json
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CameraZoomSyncTest {
    private val packets = mutableListOf<Packet>()
    private var writable = true
    private val client = object : NetworkClient {
        override val writable get() = this@CameraZoomSyncTest.writable
        override val address = InetSocketAddress("localhost", 0)
        override fun send(packet: Packet) { packets.add(packet) }
        override fun disconnect() {}
        override fun poll(): Packet? = null
        override fun enqueueWork(runnable: Runnable) { runnable.run() }
    }
    private val registries = Registries(
        TileRegistry(Json), TransitionRegistry(Json), EntityRegistry(Json),
        ComponentRegistry(Json), GridRegistry(Json), SoundRegistry(Json), CustomRegistries(Json)
    )
    private val chunks = ChunkViewManager(TransitionResolver(registries))
    private val dimensions = DimensionManager()
    private val entities = EntityManager()
    private val players = PlayerManager(dimensions, chunks, Json, entities, MainThreadDispatcher())
    private val player = players.createPlayer(client)
    private val world = World(Grid(), CollisionResolver(registries, chunks), dimensions, entities, chunks, players)

    private fun drainChunks(expected: Int) {
        repeat(1000) {
            player.syncManager.update()
            if (player.syncManager.syncedChunks.size == expected) return
        }
        assertEquals(expected, player.syncManager.syncedChunks.size)
    }

    @Test
    fun `streaming is bounded resumes without camera movement and starts at the camera`() {
        val coordinate = Coordinate(-17, 33, 2)
        player.camera.focusCoordinate(Dimension(registries, world), coordinate)
        player.camera.zoom = 0.25f
        val sync = player.syncManager
        sync.update()
        assertTrue(sync.syncedChunks.size in 1..64)
        assertEquals(ChunkWindow.at(coordinate, 16), sync.syncedChunks.first())

        assertFalse(sync.coordinateDirty)
        drainChunks(845)
    }

    @Test
    fun `backpressure pauses streaming and camera changes discard obsolete pending chunks`() {
        val dimension = Dimension(registries, world)
        player.camera.focusCoordinate(dimension, Coordinate.Zero)
        player.camera.zoom = 0.25f
        val sync = player.syncManager
        sync.update()
        val sent = sync.syncedChunks.toSet()
        writable = false
        repeat(3) { sync.update() }
        assertEquals(sent, sync.syncedChunks)
        val next = Coordinate(1000, 1000, 10)
        player.camera.focusCoordinate(dimension, next)
        sync.update()
        assertTrue(sync.syncedChunks.isEmpty())
        writable = true
        sync.update()
        assertEquals(ChunkWindow.at(next, 16), sync.syncedChunks.first())
        drainChunks(845)
        assertTrue(sync.syncedChunks.all { it.isInRange(next, 6, 2) })
        player.camera.focusCoordinate(Dimension(registries, world), Coordinate.Zero)
        sync.update()
        assertTrue(sync.syncedChunks.size in 1..64)
        assertEquals(ChunkWindow.at(Coordinate.Zero, 16), sync.syncedChunks.first())
    }

    @Test
    fun `chunk packets are limited and unsent chunks include edits in their eventual snapshot`() {
        val dimension = Dimension(registries, world)
        val layer = DenseMapLayer("base", registries)
        dimension.mapTree.layers.add(layer)
        val tile = TileDefinition(Identifier.parse("test:tile")).apply { id = 1 }
        for (z in -2..2) for (x in -1..1) for (y in -1..1) {
            layer.placeTile(Coordinate(x * 16, y * 16, z), tile)
        }
        player.camera.focusCoordinate(dimension, Coordinate.Zero)
        val sync = player.syncManager
        sync.update()
        val firstPackets = packets.filterIsInstance<MapChunkPacket>()
        assertTrue(firstPackets.size in 1..8)
        assertEquals(Coordinate.Zero, firstPackets.first().let { Coordinate(it.x, it.y, it.z) })
        val distant = Coordinate(16, 16, -2)
        assertFalse(ChunkWindow.at(distant, 16) in sync.syncedChunks)
        layer.replaceTiles(distant, tile.copy().apply { id = 2 })
        sync.tileUpdated(distant)
        assertTrue(packets.filterIsInstance<UpdateMapTilesPacket>().isEmpty())
        drainChunks(45)
        val snapshot = packets.filterIsInstance<MapChunkPacket>().single {
            it.x == distant.x && it.y == distant.y && it.z == distant.z
        }
        assertEquals(2, snapshot.baseTiles[snapshot.padding * (snapshot.width + 2 * snapshot.padding) + snapshot.padding])
        assertEquals(45, packets.filterIsInstance<MapChunkPacket>().size)
    }

    @Test
    fun `nearby roofs arrive before distant ground and lower floors`() {
        val dimension = Dimension(registries, world)
        val layer = DenseMapLayer("base", registries)
        dimension.mapTree.layers.add(layer)
        val tile = TileDefinition(Identifier.parse("test:tile")).apply { id = 1 }
        for (z in -2..2) for (x in -6..6) for (y in -6..6) {
            layer.placeTile(Coordinate(x * 16, y * 16, z), tile)
        }
        player.camera.focusCoordinate(dimension, Coordinate.Zero)
        player.camera.zoom = 0.25f
        drainChunks(845)
        val snapshots = packets.filterIsInstance<MapChunkPacket>()
        assertEquals(listOf(0, 1, 2, -1, -2), snapshots.take(5).map { it.z })
        assertTrue(snapshots.take(5).all { it.x == 0 && it.y == 0 })
        assertTrue(snapshots.indexOfFirst { it.z == 2 } < snapshots.indexOfFirst { it.x == 96 })
        val distances = snapshots.map { maxOf(kotlin.math.abs(it.x), kotlin.math.abs(it.y)) }
        assertEquals(distances.sorted(), distances)
    }

    @Test
    fun `empty windows do not consume the populated packet allowance`() {
        val dimension = Dimension(registries, world)
        val layer = DenseMapLayer("roof", registries)
        dimension.mapTree.layers.add(layer)
        val tile = TileDefinition(Identifier.parse("test:tile")).apply { id = 1 }
        layer.placeTile(Coordinate(0, 0, 2), tile)
        player.camera.focusCoordinate(dimension, Coordinate.Zero)
        player.camera.zoom = 0.25f
        val sync = PlayerSyncManager(chunks, Json, player, entities, nanoTime = { 0L })
        sync.update()
        // This includes the nearby roof despite the empty floors below it.
        assertEquals(64, sync.syncedChunks.size)
        assertEquals(listOf(2), packets.filterIsInstance<MapChunkPacket>().map { it.z })
        repeat(13) {
            val previous = sync.syncedChunks.size
            packets.clear()
            sync.update()
            assertTrue(sync.syncedChunks.size - previous <= 64)
            assertTrue(packets.filterIsInstance<MapChunkPacket>().size <= 8)
        }
        assertEquals(845, sync.syncedChunks.size)
    }

    @Test
    fun `empty scanning still respects the CPU budget`() {
        player.camera.focusCoordinate(Dimension(registries, world), Coordinate.Zero)
        player.camera.zoom = 0.25f
        var time = 0L
        val sync = PlayerSyncManager(chunks, Json, player, entities, nanoTime = {
            time += 2_000_000L
            time
        })
        sync.update()
        assertEquals(2, sync.syncedChunks.size)
        assertTrue(packets.filterIsInstance<MapChunkPacket>().isEmpty())
    }

    @Test
    fun `zoom refreshes chunk coverage without moving and releases distant chunks on reset`() {
        val coordinate = Coordinate(-17, 33, 2)
        player.camera.focusCoordinate(Dimension(registries, world), coordinate)
        val sync = player.syncManager
        drainChunks(45)
        val originalChunks = sync.syncedChunks.toSet()
        assertEquals(45, originalChunks.size)
        assertEquals(64, sync.entitySyncRadius)

        player.api.setCameraZoom(0.5f)
        assertTrue(sync.coordinateDirty)
        assertEquals(3, sync.chunkViewRange)
        assertEquals(128, sync.entitySyncRadius)
        drainChunks(245)
        assertEquals(245, sync.syncedChunks.size)
        assertTrue(sync.syncedChunks.containsAll(originalChunks))
        assertEquals(coordinate, player.camera.coordinate)
        assertTrue(sync.syncedChunks.all { it.z in 0..4 })
        assertFalse(sync.coordinateDirty)

        packets.clear()
        player.api.setCameraZoom(1f)
        sync.update()
        assertEquals(originalChunks, sync.syncedChunks)
        assertEquals(200, packets.filterIsInstance<RemoveMapChunkPacket>().size)
        assertEquals(64, sync.entitySyncRadius)
    }

    @Test
    fun `zoom is bounded and unchanged zoom does not resync`() {
        val sync = player.syncManager
        player.api.setCameraZoom(0.01f)
        assertEquals(0.25f, player.camera.zoom)
        assertEquals(6, sync.chunkViewRange)
        assertEquals(256, sync.entitySyncRadius)
        sync.coordinateDirty = false
        player.api.setCameraZoom(0.25f)
        assertFalse(sync.coordinateDirty)

        player.api.setCameraZoom(2f)
        assertEquals(1f, player.camera.zoom)
        for (invalid in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { player.api.setCameraZoom(invalid) }
            assertEquals(1f, player.camera.zoom)
        }
    }
}

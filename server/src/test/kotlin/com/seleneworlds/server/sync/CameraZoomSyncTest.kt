package com.seleneworlds.server.sync

import com.seleneworlds.common.data.custom.CustomRegistries
import com.seleneworlds.common.entities.EntityRegistry
import com.seleneworlds.common.entities.component.ComponentRegistry
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
    private val client = object : NetworkClient {
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

    @Test
    fun `zoom refreshes chunk coverage without moving and releases distant chunks on reset`() {
        val coordinate = Coordinate(-17, 33, 2)
        player.camera.focusCoordinate(Dimension(registries, world), coordinate)
        val sync = player.syncManager
        sync.update()
        val originalChunks = sync.syncedChunks.toSet()
        assertEquals(45, originalChunks.size)
        assertEquals(64, sync.entitySyncRadius)

        player.api.setCameraZoom(0.5f)
        assertTrue(sync.coordinateDirty)
        assertEquals(3, sync.chunkViewRange)
        assertEquals(128, sync.entitySyncRadius)
        sync.update()
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

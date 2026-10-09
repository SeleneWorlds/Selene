package com.seleneworlds.server.sync

import kotlin.math.ceil
import kotlin.math.abs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.packet.*
import com.seleneworlds.common.grid.ChunkWindow
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.server.cameras.Camera
import com.seleneworlds.server.cameras.CameraListener
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.entities.Entity
import com.seleneworlds.server.entities.EntityManager
import com.seleneworlds.server.players.Player

class PlayerSyncManager(
    private val chunkViewManager: ChunkViewManager,
    private val json: Json,
    val player: Player,
    private val entityManager: EntityManager,
    private val nanoTime: () -> Long = System::nanoTime
) : CameraListener {
    var initialSync = false
    val syncedChunks = mutableSetOf<ChunkWindow>()
    private val syncedEntities = mutableSetOf<Int>()
    // Scale the full chunk window diameter, including the central chunk.
    val chunkViewRange get() = ceil((3.0 / player.camera.zoom - 1.0) / 2.0).toInt()
    val verticalChunkViewRange = 2
    val entitySyncRadius get() = ceil(64.0 / player.camera.zoom).toInt()

    private val pendingChunks = ArrayDeque<ChunkWindow>()

    var dimensionDirty = false
    var coordinateDirty = false

    fun update() {
        if (!initialSync) {
            refreshPendingChunks()
            syncNearbyEntities()
            initialSync = true
            dimensionDirty = false
            coordinateDirty = false
        } else if (dimensionDirty) {
            syncedChunks.forEach {
                player.client.send(RemoveMapChunkPacket(it.x, it.y, it.z, it.width, it.height))
            }
            syncedChunks.clear()
            syncedEntities.forEach {
                player.client.send(RemoveEntityPacket(it))
            }
            syncedEntities.clear()
            refreshPendingChunks()
            syncNearbyEntities()
            dimensionDirty = false
            coordinateDirty = false
        } else if (coordinateDirty) {
            val iterator = syncedChunks.iterator()
            while (iterator.hasNext()) {
                val window = iterator.next()
                if (!shouldSync(window)) {
                    iterator.remove()
                    player.client.send(RemoveMapChunkPacket(window.x, window.y, window.z, window.width, window.height))
                }
            }
            refreshPendingChunks()
            syncNearbyEntities()
            coordinateDirty = false
        }
        sendMissingChunks()
    }

    private fun refreshPendingChunks() {
        pendingChunks.clear()
        if (player.camera.dimension == null) return
        val coordinate = player.camera.coordinate
        val center = ChunkWindow.at(coordinate, chunkViewManager.chunkSize)
        pendingChunks.addAll(ChunkWindow.around(
            coordinate, chunkViewManager.chunkSize, chunkViewRange, verticalChunkViewRange
        ).filter { it !in syncedChunks }.sortedWith(
            // Complete nearby columns across floors before moving to distant ground.
            compareBy<ChunkWindow> { maxOf(abs(it.x - center.x), abs(it.y - center.y)) }
                .thenBy { it.x }
                .thenBy { it.y }
                .thenBy {
                    val dz = it.z - center.z
                    when {
                        dz == 0 -> 0
                        dz > 0 -> dz
                        else -> verticalChunkViewRange - dz
                    }
                }
        ))
    }

    fun sendMissingChunks() {
        val dimension = player.camera.dimension ?: return
        val startedAt = nanoTime()
        var processed = 0
        var sent = 0
        var bytesSent = 0
        // Empty windows count against CPU and scan limits, but not the packet limit.
        // Otherwise sparse floors can consume many ticks without sending anything.
        while (pendingChunks.isNotEmpty() && player.client.writable &&
            processed < 64 && sent < 8 && bytesSent < 32 * 1024 &&
            (processed == 0 || nanoTime() - startedAt < 4_000_000L)) {
            val window = pendingChunks.removeFirst()
            processed++
            val chunk = chunkViewManager.atWindow(dimension, player.camera, window)
            if (!chunk.isEmptyForTransfer()) {
                // Include the packet id and the relative-coordinate tile entries.
                val packetBytes = 23 + window.width * window.height * 4 + chunk.additionalTiles.size() * 10
                // A single indivisible packet may exceed the byte or time budget.
                if (bytesSent > 0 && bytesSent + packetBytes > 32 * 1024) {
                    pendingChunks.addFirst(window)
                    break
                }
                player.client.send(
                    MapChunkPacket(
                        window.x,
                        window.y,
                        window.z,
                        window.width,
                        window.height,
                        chunk.padding,
                        chunk.baseTiles,
                        chunk.additionalTiles
                    )
                )
                sent++
                bytesSent += packetBytes
            }
            syncedChunks.add(window)
        }
    }

    private fun syncNearbyEntities() {
        val iterator = syncedEntities.iterator()
        while (iterator.hasNext()) {
            val networkId = iterator.next()
            val entity = entityManager.getEntityByNetworkId(networkId)
            if (entity == null || !shouldSync(entity)) {
                iterator.remove()
                player.client.send(RemoveEntityPacket(networkId))
            }
        }
        player.camera.dimension?.let { dimension ->
            entityManager.getNearbyEntities(player.camera.coordinate, dimension, entitySyncRadius)
                .asSequence()
                .filter { shouldSync(it) }
                .forEach { syncEntity(it) }
        }
    }

    fun sendIfWatching(networkId: Int, packet: Packet) {
        if (syncedEntities.contains(networkId)) {
            player.client.send(packet)
        }
    }

    fun sendIfWatching(coordinate: Coordinate, packet: Packet) {
        if (ChunkWindow.at(coordinate, chunkViewManager.chunkSize) in syncedChunks) {
            player.client.send(packet)
        }
    }

    fun updateEntityWatch(entity: Entity) {
        if (shouldSync(entity)) {
            syncEntity(entity)
        } else {
            unsyncEntity(entity)
        }
    }

    fun updateEntity(entity: Entity) {
        if (entity.networkId in syncedEntities) {
            sendEntity(entity)
        }
    }

    private fun shouldSync(window: ChunkWindow): Boolean {
        return window.isInRange(player.camera.coordinate, chunkViewRange, verticalChunkViewRange)
    }

    private fun shouldSync(entity: Entity): Boolean {
        if (entity.dimension != player.camera.dimension) {
            return false
        }

        if (entity.coordinate.horizontalDistanceTo(player.camera.coordinate) > entitySyncRadius) {
            return false
        }

        return player.controlledEntity == entity || player.camera.canView(entity)
    }

    private fun syncEntity(entity: Entity) {
        if (entity.transient || syncedEntities.add(entity.networkId)) {
            sendEntity(entity)
        }
    }

    private fun sendEntity(entity: Entity) {
        player.client.send(
            EntityPacket(
                networkId = entity.networkId,
                entityId = entity.entityDefinition.id,
                coordinate = entity.coordinate,
                facing = entity.facing?.angle ?: 0f,
                components = entity.resolveComponentsFor(player)
                    .mapValues { json.encodeToString(it.value) }
            )
        )
    }

    private fun unsyncEntity(entity: Entity) {
        if (syncedEntities.remove(entity.networkId)) {
            player.client.send(RemoveEntityPacket(entity.networkId))
        }
    }

    override fun cameraDimensionChanged(
        camera: Camera,
        oldDimension: Dimension?,
        dimension: Dimension?
    ) {
        oldDimension?.syncManager?.playerSyncManagers?.remove(this)
        dimension?.syncManager?.playerSyncManagers?.add(this)
        dimensionDirty = true
    }

    override fun cameraCoordinateChanged(
        camera: Camera,
        prev: Coordinate,
        value: Coordinate
    ) {
        coordinateDirty = true
    }

    override fun cameraZoomChanged(camera: Camera, prev: Float, value: Float) {
        coordinateDirty = true
    }

    fun tileUpdated(coordinate: Coordinate) {
        val dimension = player.camera.dimension ?: return
        val window = ChunkWindow.at(coordinate, chunkViewManager.chunkSize)
        if (window !in syncedChunks) return
        val chunk = chunkViewManager.atWindow(dimension, player.camera, window)
        player.client.send(
            UpdateMapTilesPacket(
                coordinate,
                chunk.getBaseTileAt(coordinate),
                chunk.getAdditionalTilesAt(coordinate)
            )
        )
    }

}

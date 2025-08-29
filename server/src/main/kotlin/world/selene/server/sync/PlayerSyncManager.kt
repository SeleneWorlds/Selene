package world.selene.server.sync

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.network.Packet
import world.selene.common.network.packet.EntityPacket
import world.selene.common.network.packet.MapChunkPacket
import world.selene.common.network.packet.RemoveEntityPacket
import world.selene.common.network.packet.RemoveMapChunkPacket
import world.selene.common.network.packet.UpdateMapTilesPacket
import world.selene.common.util.ChunkWindow
import world.selene.common.util.Coordinate
import world.selene.server.cameras.Camera
import world.selene.server.cameras.CameraListener
import world.selene.server.data.Registries
import world.selene.server.dimensions.Dimension
import world.selene.server.entities.Entity
import world.selene.server.entities.EntityManager
import world.selene.server.player.Player

class PlayerSyncManager(
    private val chunkViewManager: ChunkViewManager,
    private val objectMapper: ObjectMapper,
    val player: Player,
    private val registries: Registries,
    private val entityManager: EntityManager
) : CameraListener {
    var initialSync = false
    val syncedChunks = mutableSetOf<ChunkWindow>()
    private val syncedEntities = mutableSetOf<Int>()
    val chunkViewRange = 1
    val verticalChunkViewRange = 2
    val entitySyncRadius = 64

    var dimensionDirty = false
    var coordinateDirty = false

    fun update() {
        if (!initialSync) {
            sendMissingChunks()
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
            sendMissingChunks()
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
            sendMissingChunks()
            syncNearbyEntities()
            coordinateDirty = false
        }
    }

    fun sendMissingChunks() {
        val dimension = player.camera.dimension ?: return
        val windows = ChunkWindow.around(
            player.camera.coordinate,
            chunkViewManager.chunkSize,
            chunkViewRange,
            verticalChunkViewRange
        )
        windows.asSequence().filter { it !in syncedChunks }.forEach { window ->
            val chunk = chunkViewManager.atWindow(dimension, player.camera, window)
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
            syncedChunks.add(window)
        }
    }

    private fun syncNearbyEntities() {
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
        if (shouldSync(ChunkWindow.at(coordinate, chunkViewManager.chunkSize))) {
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

    private fun shouldSync(window: ChunkWindow): Boolean {
        return window.isInRange(player.camera.coordinate, chunkViewRange, verticalChunkViewRange)
    }

    private fun shouldSync(entity: Entity): Boolean {
        if (entity.dimension != player.camera.dimension || !player.camera.canView(entity)) {
            return false
        }

        if (entity.coordinate.horizontalDistanceTo(player.camera.coordinate) > entitySyncRadius) {
            return false
        }

        return true
    }

    private fun syncEntity(entity: Entity) {
        if (entity.transient || syncedEntities.add(entity.networkId)) {
            player.client.send(
                EntityPacket(
                    networkId = entity.networkId,
                    entityId = entity.entityDefinition?.id ?: 0,
                    coordinate = entity.coordinate,
                    facing = entity.facing?.angle ?: 0f,
                    components = entity.resolveComponentsFor(player)
                        .mapValues { objectMapper.writeValueAsString(it.value) }
                )
            )
        }
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

    fun tileUpdated(coordinate: Coordinate) {
        val dimension = player.camera.dimension ?: return
        val window = ChunkWindow.at(coordinate, chunkViewManager.chunkSize)
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
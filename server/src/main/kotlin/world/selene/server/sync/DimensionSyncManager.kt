package world.selene.server.sync

import world.selene.common.grid.Grid
import world.selene.common.network.Packet
import world.selene.common.network.packet.MoveEntityPacket
import world.selene.common.util.Coordinate
import world.selene.server.entities.Entity

class DimensionSyncManager {
    val playerSyncManagers = mutableListOf<PlayerSyncManager>()

    fun updateEntityWatches(entity: Entity) {
        playerSyncManagers.forEach { it.updateEntityWatch(entity) }
    }

    fun sendToAllWatching(networkId: Int, packet: Packet) {
        for (manager in playerSyncManagers) {
            manager.sendIfWatching(networkId, packet)
        }
    }

    fun sendToAllWatching(coordinate: Coordinate, packet: Packet) {
        for (manager in playerSyncManagers) {
            manager.sendIfWatching(coordinate, packet)
        }
    }

    fun sendToAll(packet: Packet) {
        playerSyncManagers.forEach { it.player.client.send(packet) }
    }

    fun entityAdded(entity: Entity) {
        updateEntityWatches(entity)
    }

    fun entityTurned(entity: Entity, direction: Grid.Direction) {
        sendToAllWatching(
            entity.networkId,
            MoveEntityPacket(entity.networkId, entity.coordinate, entity.coordinate, direction.angle, 0f)
        )
    }

    fun entityMoved(entity: Entity, start: Coordinate, end: Coordinate, duration: Float) {
        updateEntityWatches(entity)
        sendToAllWatching(
            entity.networkId,
            MoveEntityPacket(entity.networkId, start, end, entity.facing?.angle ?: 0f, duration)
        )
    }

    fun entityTeleported(entity: Entity) {
        updateEntityWatches(entity)
        sendToAllWatching(
            entity.networkId,
            MoveEntityPacket(entity.networkId, entity.coordinate, entity.coordinate, entity.facing?.angle ?: 0f, 0f)
        )
    }

    fun tileUpdated(coordinate: Coordinate) {
        for (manager in playerSyncManagers) {
            manager.tileUpdated(coordinate)
        }
    }

}
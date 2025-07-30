package world.selene.server.sync

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

    fun entityMoved(entity: Entity, start: Coordinate, end: Coordinate, duration: Float) {
        updateEntityWatches(entity)
        sendToAllWatching(
            entity.networkId,
            MoveEntityPacket(entity.networkId, start, end, duration)
        )
    }

    fun entityTeleported(entity: Entity) {
        updateEntityWatches(entity)
        sendToAllWatching(
            entity.networkId,
            MoveEntityPacket(entity.networkId, entity.coordinate, entity.coordinate, 0f)
        )
    }
}
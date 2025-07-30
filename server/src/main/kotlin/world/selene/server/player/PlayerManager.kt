package world.selene.server.player

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.server.sync.ChunkViewManager
import world.selene.server.data.Registries
import world.selene.server.entities.EntityManager
import world.selene.server.network.NetworkClient
import world.selene.server.sync.PlayerSyncManager

class PlayerManager(private val chunkViewManager: ChunkViewManager, private val objectMapper: ObjectMapper, private val registries: Registries, private val entityManager: EntityManager) {
    fun createPlayer(client: NetworkClient): Player {
        return Player(this, client)
    }

    fun createSyncManager(player: Player): PlayerSyncManager {
        return PlayerSyncManager(chunkViewManager, objectMapper, player, registries, entityManager)
    }
}
package com.seleneworlds.server.players

import com.fasterxml.jackson.databind.ObjectMapper
import com.seleneworlds.common.threading.MainThreadDispatcher
import com.seleneworlds.common.util.ReferenceResolver
import com.seleneworlds.server.dimensions.DimensionManager
import com.seleneworlds.server.entities.EntityManager
import com.seleneworlds.server.network.NetworkClient
import com.seleneworlds.server.sync.ChunkViewManager
import com.seleneworlds.server.sync.PlayerSyncManager
import java.util.concurrent.ConcurrentHashMap

class PlayerManager(
    private val dimensionManager: DimensionManager,
    private val chunkViewManager: ChunkViewManager,
    private val objectMapper: ObjectMapper,
    private val entityManager: EntityManager,
    private val mainThreadDispatcher: MainThreadDispatcher
) : ReferenceResolver<String, Player> {
    private val _players = ConcurrentHashMap<NetworkClient, Player>()
    val players: Collection<Player> get() = _players.values

    fun createPlayer(client: NetworkClient): Player {
        val player = Player(dimensionManager, this, client)
        _players[client] = player
        return player
    }

    fun removePlayer(client: NetworkClient) {
        val player = _players.remove(client)
        if (player != null) {
            val dimension = player.camera.dimension
            dimension?.syncManager?.playerSyncManagers?.remove(player.syncManager)
            mainThreadDispatcher.runOnMainThread {
                PlayerEvents.PlayerLeft.EVENT.invoker().playerLeft(player.api)
            }
        }
    }

    fun createSyncManager(player: Player): PlayerSyncManager {
        return PlayerSyncManager(chunkViewManager, objectMapper, player, entityManager)
    }

    override fun dereferencePersisted(id: String): Player? {
        return _players.values.find { it.userId == id }
    }
}

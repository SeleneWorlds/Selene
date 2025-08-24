package world.selene.server.player

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.lua.LuaReferenceResolver
import world.selene.server.sync.ChunkViewManager
import world.selene.server.data.Registries
import world.selene.server.entities.EntityManager
import world.selene.server.network.NetworkClient
import world.selene.server.sync.PlayerSyncManager
import java.util.concurrent.ConcurrentHashMap

class PlayerManager(
    private val chunkViewManager: ChunkViewManager,
    private val objectMapper: ObjectMapper,
    private val registries: Registries,
    private val entityManager: EntityManager
) : LuaReferenceResolver<String, Player> {
    private val _players = ConcurrentHashMap<NetworkClient, Player>()
    val players: Collection<Player> get() = _players.values

    fun createPlayer(client: NetworkClient): Player {
        val player = Player(this, client)
        _players[client] = player
        return player
    }

    fun removePlayer(client: NetworkClient) {
        _players.remove(client)
    }

    fun createSyncManager(player: Player): PlayerSyncManager {
        return PlayerSyncManager(chunkViewManager, objectMapper, player, registries, entityManager)
    }

    override fun luaDereference(id: String): Player? {
        return _players.values.find { it.userId == id }
    }
}
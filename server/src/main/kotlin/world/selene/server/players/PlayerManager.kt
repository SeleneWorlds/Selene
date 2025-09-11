package world.selene.server.players

import com.fasterxml.jackson.databind.ObjectMapper
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaReferenceResolver
import world.selene.server.dimensions.DimensionManager
import world.selene.server.entities.EntityManager
import world.selene.server.lua.ServerLuaSignals
import world.selene.server.network.NetworkClient
import world.selene.server.sync.ChunkViewManager
import world.selene.server.sync.PlayerSyncManager
import java.util.concurrent.ConcurrentHashMap

class PlayerManager(
    private val dimensionManager: DimensionManager,
    private val chunkViewManager: ChunkViewManager,
    private val objectMapper: ObjectMapper,
    private val entityManager: EntityManager,
    private val luaSignals: ServerLuaSignals
) : LuaReferenceResolver<String, Player> {
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
            luaSignals.playerLeft.emit { lua ->
                lua.push(player, Lua.Conversion.NONE)
                1
            }
        }
    }

    fun createSyncManager(player: Player): PlayerSyncManager {
        return PlayerSyncManager(chunkViewManager, objectMapper, player, entityManager)
    }

    override fun luaDereference(id: String): Player? {
        return _players.values.find { it.userId == id }
    }
}
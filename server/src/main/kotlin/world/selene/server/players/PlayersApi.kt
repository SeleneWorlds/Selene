package world.selene.server.players

import world.selene.server.network.NetworkClientImpl
import world.selene.server.network.NetworkServer

class PlayersApi(private val networkServer: NetworkServer) {

    /**
     * Returns a list of all currently online players.
     *
     * ```signatures
     * GetOnlinePlayers() -> table[Player]
     * ```
     */
    fun getOnlinePlayers(): List<PlayerApi> {
        return networkServer.clients
            .filterIsInstance<NetworkClientImpl>()
            .map { it.player.api }
    }

}

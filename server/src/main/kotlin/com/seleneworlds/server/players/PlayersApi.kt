package com.seleneworlds.server.players

import com.seleneworlds.server.network.NetworkPlayerClient
import com.seleneworlds.server.network.NetworkServer

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
            .filterIsInstance<NetworkPlayerClient>()
            .map { it.player.api }
    }

}

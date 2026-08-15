package com.seleneworlds.server.network

import com.seleneworlds.server.players.Player

interface NetworkPlayerClient : NetworkClient {
    val player: Player
}
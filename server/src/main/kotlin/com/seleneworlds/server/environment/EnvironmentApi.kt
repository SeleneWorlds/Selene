package com.seleneworlds.server.environment

import com.seleneworlds.common.network.packet.SetEnvironmentLightPacket
import com.seleneworlds.server.players.PlayerApi
import com.seleneworlds.server.players.PlayersApi

class EnvironmentApi(private val players: PlayersApi) {
    fun setAmbientLight(player: PlayerApi, red: Float, green: Float, blue: Float) {
        player.delegate.client.send(packet(red, green, blue))
    }

    fun setGlobalAmbientLight(red: Float, green: Float, blue: Float) {
        val packet = packet(red, green, blue)
        players.getOnlinePlayers().forEach { it.delegate.client.send(packet) }
    }

    private fun packet(red: Float, green: Float, blue: Float) = SetEnvironmentLightPacket(
        red.coerceIn(0f, 1f),
        green.coerceIn(0f, 1f),
        blue.coerceIn(0f, 1f),
    )
}

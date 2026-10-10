package com.seleneworlds.server.environment

import com.seleneworlds.common.network.packet.SetEnvironmentLightPacket
import com.seleneworlds.server.players.PlayerApi
import com.seleneworlds.server.players.PlayersApi

class EnvironmentApi(private val players: PlayersApi) {
    fun setAmbientLight(player: PlayerApi, name: String, red: Float, green: Float, blue: Float) {
        player.delegate.client.send(packet(name, red, green, blue))
    }

    fun setGlobalAmbientLight(name: String, red: Float, green: Float, blue: Float) {
        val packet = packet(name, red, green, blue)
        players.getOnlinePlayers().forEach { it.delegate.client.send(packet) }
    }

    fun setOutdoorsAmbientLight(player: PlayerApi, red: Float, green: Float, blue: Float) =
        setAmbientLight(player, "outdoors", red, green, blue)

    fun setGlobalOutdoorsAmbientLight(red: Float, green: Float, blue: Float) =
        setGlobalAmbientLight("outdoors", red, green, blue)

    fun setIndoorsAmbientLight(player: PlayerApi, red: Float, green: Float, blue: Float) =
        setAmbientLight(player, "indoors", red, green, blue)

    fun setGlobalIndoorsAmbientLight(red: Float, green: Float, blue: Float) =
        setGlobalAmbientLight("indoors", red, green, blue)

    fun setUndergroundAmbientLight(player: PlayerApi, red: Float, green: Float, blue: Float) =
        setAmbientLight(player, "underground", red, green, blue)

    fun setGlobalUndergroundAmbientLight(red: Float, green: Float, blue: Float) =
        setGlobalAmbientLight("underground", red, green, blue)

    private fun packet(name: String, red: Float, green: Float, blue: Float) = SetEnvironmentLightPacket(
        name,
        red.coerceIn(0f, 1f),
        green.coerceIn(0f, 1f),
        blue.coerceIn(0f, 1f),
    )
}

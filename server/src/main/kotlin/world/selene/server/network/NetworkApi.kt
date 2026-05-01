package world.selene.server.network

import com.fasterxml.jackson.databind.ObjectMapper
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.util.CallerInfo
import world.selene.common.network.LuaPayloadRegistry
import world.selene.common.network.packet.CustomPayloadPacket
import world.selene.server.entities.EntityApi
import world.selene.server.players.Player
import world.selene.server.players.PlayerApi

/**
 * Send and handle custom payloads.
 */
@Suppress("SameReturnValue")
class NetworkApi(
    private val payloadRegistry: LuaPayloadRegistry,
    private val objectMapper: ObjectMapper
) {
    fun handlePayload(payloadId: String, callback: LuaValue, registrationSite: CallerInfo) {
        payloadRegistry.registerHandler(payloadId, callback, registrationSite)
    }

    fun sendToPlayer(player: PlayerApi, payloadId: String, payload: Any?) {
        player.delegate.client.send(CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload)))
    }

    fun sendToPlayers(players: List<*>, payloadId: String, payload: Any?) {
        val packet = CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload))
        players.forEach { player ->
            (player as? Player)?.client?.send(packet)
        }
    }

    fun sendToEntity(entity: EntityApi, payloadId: String, payload: Any?) {
        val packet = CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload))
        entity.entity.getControllingPlayers().forEach { player ->
            player.client.send(packet)
        }
    }

    fun sendToEntities(entities: List<*>, payloadId: String, payload: Any?) {
        val packet = CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload))
        entities.forEach { entity ->
            (entity as? EntityApi)?.entity?.getControllingPlayers()?.forEach { player ->
                player.client.send(packet)
            }
        }
    }
}

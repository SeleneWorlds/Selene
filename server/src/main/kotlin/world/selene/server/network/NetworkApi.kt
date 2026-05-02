package world.selene.server.network

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.network.PayloadHandlerRegistry
import world.selene.common.network.packet.CustomPayloadPacket
import world.selene.server.entities.EntityApi
import world.selene.server.players.Player
import world.selene.server.players.PlayerApi

/**
 * Send and handle custom payloads.
 */
class NetworkApi(
    private val payloadRegistry: PayloadHandlerRegistry<Player>,
    private val objectMapper: ObjectMapper
) {
    fun handlePayload(payloadId: String, callback: (Player, Map<*, *>) -> Unit) {
        payloadRegistry.registerHandler(payloadId, callback)
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

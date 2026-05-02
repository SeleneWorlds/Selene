package com.seleneworlds.server.network

import com.seleneworlds.common.network.PayloadHandlerRegistry
import com.seleneworlds.common.network.packet.CustomPayloadPacket
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.SerializedMapSerializer
import com.seleneworlds.server.entities.EntityApi
import com.seleneworlds.server.players.Player
import com.seleneworlds.server.players.PlayerApi
import kotlinx.serialization.json.Json

/**
 * Send and handle custom payloads.
 */
class NetworkApi(
    private val payloadRegistry: PayloadHandlerRegistry<Player>,
    private val json: Json
) {
    fun handlePayload(payloadId: String, callback: (Player, SerializedMap) -> Unit) {
        payloadRegistry.registerHandler(payloadId, callback)
    }

    fun sendToPlayer(player: PlayerApi, payloadId: String, payload: SerializedMap) {
        player.delegate.client.send(
            CustomPayloadPacket(
                payloadId,
                json.encodeToString(SerializedMapSerializer, payload)
            )
        )
    }

    fun sendToPlayers(players: List<*>, payloadId: String, payload: SerializedMap) {
        val packet = CustomPayloadPacket(payloadId, json.encodeToString(SerializedMapSerializer, payload))
        players.forEach { player ->
            (player as? Player)?.client?.send(packet)
        }
    }

    fun sendToEntity(entity: EntityApi, payloadId: String, payload: SerializedMap) {
        val packet = CustomPayloadPacket(payloadId, json.encodeToString(SerializedMapSerializer,payload))
        entity.entity.getControllingPlayers().forEach { player ->
            player.client.send(packet)
        }
    }

    fun sendToEntities(entities: List<*>, payloadId: String, payload: SerializedMap) {
        val packet = CustomPayloadPacket(payloadId, json.encodeToString(SerializedMapSerializer, payload))
        entities.forEach { entity ->
            (entity as? EntityApi)?.entity?.getControllingPlayers()?.forEach { player ->
                player.client.send(packet)
            }
        }
    }
}

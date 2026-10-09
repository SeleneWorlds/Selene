package com.seleneworlds.server.network

import com.seleneworlds.common.network.PayloadHandlerRegistry
import com.seleneworlds.common.network.packet.CustomPayloadPacket
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.SerializedMapSerializer
import com.seleneworlds.server.entities.EntityApi
import com.seleneworlds.server.players.Player
import com.seleneworlds.server.players.PlayerApi
import com.seleneworlds.server.config.PacketRateLimit
import kotlinx.serialization.json.Json

/**
 * Send and handle custom payloads.
 */
class NetworkApi(
    private val payloadRegistry: PayloadHandlerRegistry<Player>,
    private val json: Json,
    private val payloadRateLimits: PayloadRateLimitRegistry
) {
    /** Used only when the administrator has not configured a limit for this payload ID. */
    fun setDefaultPayloadRateLimit(payloadId: String, packetsPerSecond: Double, burst: Int) {
        payloadRateLimits.setDefault(payloadId, PacketRateLimit(packetsPerSecond, burst))
    }

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

    fun sendToPlayers(players: List<PlayerApi>, payloadId: String, payload: SerializedMap) {
        val packet = CustomPayloadPacket(payloadId, json.encodeToString(SerializedMapSerializer, payload))
        players.forEach { player ->
            player.delegate.client.send(packet)
        }
    }

    fun sendToEntity(entity: EntityApi, payloadId: String, payload: SerializedMap) {
        val packet = CustomPayloadPacket(payloadId, json.encodeToString(SerializedMapSerializer,payload))
        entity.delegate.getControllingPlayers().forEach { player ->
            player.client.send(packet)
        }
    }

    fun sendToEntities(entities: List<*>, payloadId: String, payload: SerializedMap) {
        val packet = CustomPayloadPacket(payloadId, json.encodeToString(SerializedMapSerializer, payload))
        entities.forEach { entity ->
            (entity as? EntityApi)?.delegate?.getControllingPlayers()?.forEach { player ->
                player.client.send(packet)
            }
        }
    }
}

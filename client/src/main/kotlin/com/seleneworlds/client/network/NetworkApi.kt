package com.seleneworlds.client.network

import kotlinx.serialization.json.Json
import com.seleneworlds.common.network.PayloadHandlerRegistry
import com.seleneworlds.common.network.packet.CustomPayloadPacket
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.SerializedMapSerializer

/**
 * Send and handle custom payloads.
 */
class NetworkApi(
    private val networkClient: NetworkClient,
    private val json: Json,
    private val payloadRegistry: PayloadHandlerRegistry<Unit>
) {
    fun handlePayload(payloadId: String, callback: (SerializedMap) -> Unit) {
        payloadRegistry.registerHandler(payloadId) {
            _, payload -> callback(payload)
        }
    }

    fun sendToServer(payloadId: String, payload: SerializedMap) {
        networkClient.send(CustomPayloadPacket(payloadId, json.encodeToString(SerializedMapSerializer, payload)))
    }
}

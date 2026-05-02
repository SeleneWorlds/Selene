package world.selene.client.network

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.network.PayloadHandlerRegistry
import world.selene.common.network.packet.CustomPayloadPacket

/**
 * Send and handle custom payloads.
 */
class NetworkApi(
    private val networkClient: NetworkClient,
    private val objectMapper: ObjectMapper,
    private val payloadRegistry: PayloadHandlerRegistry<Unit>
) {
    fun handlePayload(payloadId: String, callback: (Map<*, *>) -> Unit) {
        payloadRegistry.registerHandler(payloadId) {
            _, payload -> callback(payload)
        }
    }

    fun sendToServer(payloadId: String, payload: Any?) {
        networkClient.send(CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload)))
    }
}

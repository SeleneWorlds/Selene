package world.selene.client.network

import com.fasterxml.jackson.databind.ObjectMapper
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.util.CallerInfo
import world.selene.common.network.LuaPayloadRegistry
import world.selene.common.network.packet.CustomPayloadPacket

/**
 * Send and handle custom payloads.
 */
@Suppress("SameReturnValue")
class NetworkApi(
    private val networkClient: NetworkClient,
    private val objectMapper: ObjectMapper,
    private val payloadRegistry: LuaPayloadRegistry
) {
    fun handlePayload(payloadId: String, callback: LuaValue, registrationSite: CallerInfo) {
        payloadRegistry.registerHandler(payloadId, callback, registrationSite)
    }

    fun sendToServer(payloadId: String, payload: Any?) {
        networkClient.send(CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload)))
    }
}

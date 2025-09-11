package world.selene.client.network

import com.fasterxml.jackson.databind.ObjectMapper
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.*
import world.selene.common.lua.util.checkFunction
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.getCallerInfo
import world.selene.common.lua.util.register
import world.selene.common.lua.util.toAnyMap
import world.selene.common.network.LuaPayloadRegistry
import world.selene.common.network.packet.CustomPayloadPacket

/**
 * Send and handle custom payloads.
 */
@Suppress("SameReturnValue")
class LuaClientNetworkModule(
    private val networkClient: NetworkClient,
    private val objectMapper: ObjectMapper,
    private val payloadRegistry: LuaPayloadRegistry
) : LuaModule {
    override val name = "selene.network"

    override fun register(table: LuaValue) {
        table.register("HandlePayload", this::luaHandlePayload)
        table.register("SendToServer", this::luaSendToServer)
    }

    /**
     * Registers a handler for incoming custom payloads from the server.
     *
     * ```signatures
     * HandlePayload(payloadId: string, callback: function(payload: table))
     * ```
     */
    private fun luaHandlePayload(lua: Lua): Int {
        val payloadId = lua.checkString(1)
        val callback = lua.checkFunction(2)
        val registrationSite = lua.getCallerInfo()
        payloadRegistry.registerHandler(payloadId, callback, registrationSite)
        return 0
    }

    /**
     * Sends a custom payload to the server.
     *
     * ```signatures
     * SendToServer(payloadId: string, payload: table)
     * ```
     */
    private fun luaSendToServer(lua: Lua): Int {
        val payloadId = lua.checkString(1)
        val payload = lua.toAnyMap(2)
        networkClient.send(CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload)))
        return 0
    }
}

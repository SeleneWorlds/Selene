package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.network.NetworkClient
import world.selene.common.lua.LuaModule
import world.selene.common.lua.LuaPayloadRegistry
import world.selene.common.lua.checkFunction
import world.selene.common.lua.checkString
import world.selene.common.lua.getCallerInfo
import world.selene.common.lua.register
import world.selene.common.network.packet.CustomPayloadPacket

class LuaClientNetworkModule(
    private val networkClient: NetworkClient,
    private val payloadRegistry: LuaPayloadRegistry
) : LuaModule {
    override val name = "selene.network"

    override fun register(table: LuaValue) {
        table.register("HandlePayload", this::luaHandlePayload)
        table.register("SendToServer", this::luaSendToServer)
    }

    private fun luaHandlePayload(lua: Lua): Int {
        val payloadId = lua.checkString(1)
        val callback = lua.checkFunction(2)
        val registrationSite = lua.getCallerInfo()
        payloadRegistry.registerHandler(payloadId, callback, registrationSite)
        return 0
    }

    private fun luaSendToServer(lua: Lua): Int {
        val payloadId = lua.checkString(1)
        val payload = lua.toMap(2) as Map<Any, Any>
        networkClient.send(CustomPayloadPacket(payloadId, payload))
        return 0
    }
}

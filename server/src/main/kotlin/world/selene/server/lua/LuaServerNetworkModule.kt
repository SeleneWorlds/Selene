package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.LuaPayloadRegistry
import world.selene.common.lua.checkFunction
import world.selene.common.lua.checkJavaObject
import world.selene.common.lua.checkString
import world.selene.common.lua.getCallerInfo
import world.selene.common.lua.register
import world.selene.common.network.packet.CustomPayloadPacket
import world.selene.server.player.Player

class LuaServerNetworkModule(private val payloadRegistry: LuaPayloadRegistry) : LuaModule {
    override val name = "selene.network"

    override fun register(table: LuaValue) {
        table.register("HandlePayload", this::luaHandlePayload)
        table.register("SendToPlayer", this::luaSendToPlayer)
    }

    private fun luaHandlePayload(lua: Lua): Int {
        val payloadId = lua.checkString(1)
        val callback = lua.checkFunction(2)
        val registrationSite = lua.getCallerInfo()
        payloadRegistry.registerHandler(payloadId, callback, registrationSite)
        return 0
    }

    private fun luaSendToPlayer(lua: Lua): Int {
        val player = lua.checkJavaObject<Player>(1)
        val payloadId = lua.checkString(2)
        val payload = lua.toMap(3) as Map<Any, Any>
        player.client.send(CustomPayloadPacket(payloadId, payload))
        return 0
    }


}

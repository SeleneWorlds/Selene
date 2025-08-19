package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.LuaPayloadRegistry
import world.selene.common.lua.checkJavaObject
import world.selene.common.lua.luaTableToMap
import world.selene.common.lua.register
import world.selene.common.network.packet.CustomPayloadPacket
import world.selene.server.player.Player

class LuaServerNetworkModule(private val payloadRegistry: LuaPayloadRegistry) : LuaModule {
    override val name = "selene.network"

    override fun register(table: LuaValue) {
        table.register("HandlePayload", this::luaHandlePayload)
        table.register("SendToPlayer", this::luaSendToPlayer)
    }

    private fun luaHandlePayload(lua: Lua, args: Array<LuaValue>): Array<LuaValue> {
        val payloadId = args[0].toString()
        val callback = args[1]
        payloadRegistry.registerHandler(payloadId, callback)
        return emptyArray()
    }

    private fun luaSendToPlayer(lua: Lua): Int {
        val player = lua.checkJavaObject<Player>(1)
        val payloadId = lua.toString(-2)!!
        lua.pushValue(-1)
        val payload = lua.luaTableToMap(-1)
        lua.pop(1)
        player.client.send(CustomPayloadPacket(payloadId, payload))
        return 0
    }


}

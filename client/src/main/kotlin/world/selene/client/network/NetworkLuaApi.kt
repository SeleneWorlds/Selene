package world.selene.client.network

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkFunction
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.getCallerInfo
import world.selene.common.lua.util.register
import world.selene.common.lua.util.toAnyMap

/**
 * Send and handle custom payloads.
 */
class NetworkLuaApi(private val api: NetworkApi) : LuaModule {
    override val name = "selene.network"

    override fun register(table: LuaValue) {
        table.register("HandlePayload", ::luaHandlePayload)
        table.register("SendToServer", ::luaSendToServer)
    }

    private fun luaHandlePayload(lua: Lua): Int {
        api.handlePayload(lua.checkString(1), lua.checkFunction(2), lua.getCallerInfo())
        return 0
    }

    private fun luaSendToServer(lua: Lua): Int {
        api.sendToServer(lua.checkString(1), lua.toAnyMap(2))
        return 0
    }
}

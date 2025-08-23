package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkString
import world.selene.common.lua.register
import world.selene.server.data.ServerCustomData

class LuaServerModule(
    private val signals: ServerLuaSignals,
    private val serverCustomData: ServerCustomData
) : LuaModule {
    override val name = "selene.server"

    override fun register(table: LuaValue) {
        table.set("ServerStarted", signals.serverStarted)
        table.set("ServerReloaded", signals.serverReloaded)
        table.register("GetCustomData", this::luaGetCustomData)
        table.register("SetCustomData", this::luaSetCustomData)
    }

    private fun luaGetCustomData(lua: Lua): Int {
        val key = lua.checkString(1)
        val defaultValue = lua.toObject(2)
        val value = serverCustomData.getCustomData(key, defaultValue)
        lua.push(value, Lua.Conversion.FULL)
        return 1
    }

    private fun luaSetCustomData(lua: Lua): Int {
        val key = lua.checkString(1)
        val value = lua.toObject(2)!!
        serverCustomData.setCustomData(key, value)
        return 0
    }
}

package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkString
import world.selene.common.lua.register
import world.selene.server.config.ScriptProperties

class LuaConfigModule(private val scriptProperties: ScriptProperties) : LuaModule {
    override val name = "selene.config"

    override fun register(table: LuaValue) {
        table.register("GetProperty", this::luaGetProperty)
    }

    private fun luaGetProperty(lua: Lua): Int {
        val key = lua.checkString(1)

        val value = scriptProperties.getProperty(key)
        if (value != null) {
            lua.push(value)
        } else {
            lua.pushValue(2)
        }
        return 1
    }
}

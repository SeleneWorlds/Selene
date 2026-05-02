package com.seleneworlds.server.config

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkType
import com.seleneworlds.common.lua.util.register

/**
 * Lookup properties configured in the `script.properties` file.
 */
class ConfigLuaApi(private val api: ConfigApi) : LuaModule {
    override val name = "selene.config"

    override fun register(table: LuaValue) {
        table.register("GetProperty", this::luaGetProperty)
    }

    private fun luaGetProperty(lua: Lua): Int {
        val key = lua.checkString(1)
        if (lua.top >= 2) lua.checkType(2, Lua.LuaType.STRING)

        val value = api.getProperty(key)
        if (value != null) {
            lua.push(value)
        } else {
            lua.pushValue(2)
        }
        return 1
    }
}

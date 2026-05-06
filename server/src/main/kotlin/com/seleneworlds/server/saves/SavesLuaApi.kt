package com.seleneworlds.server.saves

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register

/**
 * Save file management for persistent data storage.
 */
class SavesLuaApi(private val api: SavesApi) : LuaModule {
    override val name = "selene.saves"

    override fun register(table: LuaValue) {
        table.register("has", this::has)
        table.register("save", this::save)
        table.register("load", this::load)
    }

    private fun has(lua: Lua): Int {
        lua.push(api.has(lua.checkString(-1)))
        return 1
    }

    private fun save(lua: Lua): Int {
        val savable = lua.toJavaObject(-2)
        val path = lua.checkString(-1)
        if (savable != null) {
            api.save(savable, path)
        }
        return 0
    }

    private fun load(lua: Lua): Int {
        lua.push(api.load(lua.checkString(-1)), Lua.Conversion.FULL)
        return 1
    }
}

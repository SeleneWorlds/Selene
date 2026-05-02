package com.seleneworlds.common.grid

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkFloat
import com.seleneworlds.common.lua.util.checkInt
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register

/**
 * Define and lookup directions.
 */
open class GridLuaApi(private val api: GridApi) : LuaModule {
    override val name = "selene.grid"

    override fun register(table: LuaValue) {
        table.register("DefineDirection", this::luaDefineDirection)
        table.register("GetDirectionByName", this::luaGetDirectionByName)
    }

    private fun luaGetDirectionByName(lua: Lua): Int {
        lua.push(api.getDirectionByName(lua.checkString(1)), Lua.Conversion.NONE)
        return 1
    }

    private fun luaDefineDirection(lua: Lua): Int {
        lua.push(
            api.defineDirection(lua.checkString(1), lua.checkInt(2), lua.checkInt(3), lua.checkInt(4), lua.checkFloat(5)),
            Lua.Conversion.NONE
        )
        return 1
    }
}

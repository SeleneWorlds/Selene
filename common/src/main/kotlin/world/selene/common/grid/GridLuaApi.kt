package world.selene.common.grid

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkFloat
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.register

/**
 * Define and lookup directions.
 */
@Suppress("SameReturnValue")
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

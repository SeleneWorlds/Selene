package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.grid.Grid
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkFloat
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkString
import world.selene.common.lua.register
import world.selene.common.util.Coordinate

class LuaGridModule(private val grid: Grid) : LuaModule {
    override val name = "selene.grid"

    override fun register(table: LuaValue) {
        table.register("DefineDirection", this::luaDefineDirection)
        table.register("ScreenToCoordinate", this::luaScreenToCoordinate)
    }

    private fun luaDefineDirection(lua: Lua): Int {
        val name = lua.checkString(1)
        val x = lua.checkInt(2)
        val y = lua.checkInt(3)
        val z = lua.checkInt(4)
        lua.push(grid.defineDirection(name, Coordinate(x, y, z)), Lua.Conversion.NONE)
        return 1
    }

    private fun luaScreenToCoordinate(lua: Lua): Int {
        val x = lua.checkFloat(1)
        val y = lua.checkFloat(2)
        lua.push(grid.screenToCoordinate(x, y), Lua.Conversion.NONE)
        return 1
    }
}

package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.grid.ClientGrid
import world.selene.common.lua.LuaGridModule
import world.selene.common.lua.checkFloat
import world.selene.common.lua.register

/**
 * Defining and looking up directions.
 */
class LuaClientGridModule(private val grid: ClientGrid) : LuaGridModule(grid) {
    override val name = "selene.grid"

    override fun register(table: LuaValue) {
        super.register(table)
        table.register("ScreenToCoordinate", this::luaScreenToCoordinate)
    }

    /**
     * Converts screen coordinates to grid coordinates.
     *
     * ```signatures
     * ScreenToCoordinate(screenX: number, screenY: number) -> Coordinate
     * ```
     */
    private fun luaScreenToCoordinate(lua: Lua): Int {
        val x = lua.checkFloat(1)
        val y = lua.checkFloat(2)
        lua.push(grid.screenToCoordinate(x, y), Lua.Conversion.NONE)
        return 1
    }
}

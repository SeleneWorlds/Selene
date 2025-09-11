package world.selene.client.grid

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.grid.LuaGridModule
import world.selene.common.lua.util.checkFloat
import world.selene.common.lua.util.register

/**
 * Defining and looking up directions.
 */
@Suppress("SameReturnValue")
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
        val z = if (lua.isNumber(3)) lua.toInteger(3).toInt() else 0
        lua.push(grid.screenToCoordinate(x, y, z), Lua.Conversion.NONE)
        return 1
    }
}

package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.grid.Grid
import world.selene.common.util.Coordinate

/**
 * Provides access to grid system for defining and managing directions.
 */
open class LuaGridModule(private val grid: Grid) : LuaModule {
    override val name = "selene.grid"

    override fun register(table: LuaValue) {
        table.register("DefineDirection", this::luaDefineDirection)
        table.register("GetDirectionByName", this::luaGetDirectionByName)
    }

    /**
     * Gets a direction by its name.
     * Throws an error if the direction is not found.
     *
     * ```signatures
     * GetDirectionByName(name: string) -> Direction
     * ```
     */
    private fun luaGetDirectionByName(lua: Lua): Int {
        val name = lua.checkString(1)
        val directionByName = grid.getDirectionByName(name)
            ?: return lua.error(IllegalArgumentException("Unknown direction: $name"))
        lua.push(directionByName, Lua.Conversion.NONE)
        return 1
    }

    /**
     * Defines a new direction with a name, coordinate offset, and angle.
     *
     * ```signatures
     * DefineDirection(name: string, x: number, y: number, z: number, angle: number) -> Direction
     * ```
     */
    private fun luaDefineDirection(lua: Lua): Int {
        val name = lua.checkString(1)
        val x = lua.checkInt(2)
        val y = lua.checkInt(3)
        val z = lua.checkInt(4)
        val angle = lua.checkFloat(5)
        lua.push(grid.defineDirection(name, Coordinate(x, y, z), angle), Lua.Conversion.NONE)
        return 1
    }
}

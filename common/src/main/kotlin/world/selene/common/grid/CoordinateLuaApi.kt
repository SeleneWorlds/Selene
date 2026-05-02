package world.selene.common.grid

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.util.checkCoordinate
import world.selene.common.lua.util.checkUserdata

object CoordinateLuaApi {
    /**
     * X coordinate in the tile grid.
     *
     * ```property
     * X: number
     * ```
     */
    private fun luaGetX(lua: Lua): Int {
        val coordinate = lua.checkUserdata<Coordinate>(1)
        lua.push(coordinate.x)
        return 1
    }

    /**
     * Y coordinate in the tile grid.
     *
     * ```property
     * Y: number
     * ```
     */
    private fun luaGetY(lua: Lua): Int {
        val coordinate = lua.checkUserdata<Coordinate>(1)
        lua.push(coordinate.y)
        return 1
    }

    /**
     * Z coordinate in the tile grid.
     *
     * ```property
     * Z: number
     * ```
     */
    private fun luaGetZ(lua: Lua): Int {
        val coordinate = lua.checkUserdata<Coordinate>(1)
        lua.push(coordinate.z)
        return 1
    }

    /**
     * Calculates the horizontal distance to another coordinate.
     * Uses 2D Euclidean distance (ignoring Z coordinate).
     *
     * ```signatures
     * GetHorizontalDistanceTo(other: Coordinate) -> number
     * ```
     */
    private fun luaGetHorizontalDistanceTo(lua: Lua): Int {
        val self = lua.checkUserdata<Coordinate>(1)
        val (other, _) = lua.checkCoordinate(2)
        lua.push(self.horizontalDistanceTo(other))
        return 1
    }

    val luaMeta = LuaMappedMetatable(Coordinate::class) {
        getter(::luaGetX)
        getter(::luaGetY)
        getter(::luaGetZ)
        getter(::luaGetX, "x")
        getter(::luaGetY, "y")
        getter(::luaGetZ, "z")
        callable(::luaGetHorizontalDistanceTo)
    }
}

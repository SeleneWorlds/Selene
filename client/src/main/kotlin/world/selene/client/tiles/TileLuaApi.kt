package world.selene.client.tiles

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.util.checkUserdata

object TileLuaApi {

    /**
     * Coordinate this tile is located at.
     *
     * ```property
     * Coordinate: Coordinate
     * ```
     */
    private fun luaGetCoordinate(lua: Lua): Int {
        val tile = lua.checkUserdata<TileApi>(1)
        lua.push(tile.getCoordinate(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * Registry definition of this tile.
     *
     * ```property
     * Definition: TileDefinition
     * ```
     */
    private fun luaGetDefinition(lua: Lua): Int {
        val tile = lua.checkUserdata<TileApi>(1)
        lua.push(tile.getDefinition().get(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * Visual used to render this tile.
     *
     * ```property
     * Visual: ReloadableVisualApi
     * ```
     */
    private fun luaGetVisual(lua: Lua): Int {
        val tile = lua.checkUserdata<TileApi>(1)
        lua.push(tile.getVisual(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * X coordinate of this tile in the grid.
     *
     * ```property
     * X: number
     * ```
     */
    private fun luaGetX(lua: Lua): Int {
        val tile = lua.checkUserdata<TileApi>(1)
        lua.push(tile.getX())
        return 1
    }

    /**
     * Y coordinate of this tile in the grid.
     *
     * ```property
     * Y: number
     * ```
     */
    private fun luaGetY(lua: Lua): Int {
        val tile = lua.checkUserdata<TileApi>(1)
        lua.push(tile.getY())
        return 1
    }

    /**
     * Z coordinate of this tile in the grid.
     *
     * ```property
     * Z: number
     * ```
     */
    private fun luaGetZ(lua: Lua): Int {
        val tile = lua.checkUserdata<TileApi>(1)
        lua.push(tile.getZ())
        return 1
    }

    /**
     * Unique name of the registry definition of this tile.
     *
     * ```property
     * Name: string
     * ```
     */
    private fun luaGetName(lua: Lua): Int {
        val tile = lua.checkUserdata<TileApi>(1)
        lua.push(tile.getName())
        return 1
    }

    val luaMeta = LuaMappedMetatable(TileApi::class) {
        getter(::luaGetCoordinate)
        getter(::luaGetDefinition)
        getter(::luaGetVisual)
        getter(::luaGetX)
        getter(::luaGetY)
        getter(::luaGetZ)
        getter(::luaGetName)
    }
}

package world.selene.server.tiles

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.util.checkRegistry
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata
import world.selene.common.lua.util.throwError

object TransientTileLuaApi {

    /**
     * Registry definition of this tile.
     *
     * ```property
     * Definition: TileDefinition
     * ```
     */
    private fun luaGetDefinition(lua: Lua): Int {
        val tile = lua.checkUserdata<TransientTileApi>(1)
        lua.push(tile.getDefinition().get(), Lua.Conversion.NONE)
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
        val tile = lua.checkUserdata<TransientTileApi>(1)
        lua.push(tile.getName())
        return 1
    }

    /**
     * Dimension this tile is located in.
     *
     * ```property
     * Dimension: Dimension
     * ```
     */
    private fun luaGetDimension(lua: Lua): Int {
        val tile = lua.checkUserdata<TransientTileApi>(1)
        lua.push(tile.getDimension(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * Coordinate this tile is located at.
     *
     * ```property
     * Coordinate: Coordinate
     * ```
     */
    private fun luaGetCoordinate(lua: Lua): Int {
        val tile = lua.checkUserdata<TransientTileApi>(1)
        lua.push(tile.getCoordinate(), Lua.Conversion.NONE)
        return 1
    }

    /**
     * X coordinate in the tile grid.
     *
     * ```property
     * X: number
     * ```
     */
    private fun luaGetX(lua: Lua): Int {
        val tile = lua.checkUserdata<TransientTileApi>(1)
        lua.push(tile.getX())
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
        val tile = lua.checkUserdata<TransientTileApi>(1)
        lua.push(tile.getY())
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
        val tile = lua.checkUserdata<TransientTileApi>(1)
        lua.push(tile.getZ())
        return 1
    }

    /**
     * Gets metadata value for a specific key from this tile's definition.
     *
     * ```signatures
     * GetMetadata(key: string) -> string|nil
     * ```
     */
    private fun luaGetMetadata(lua: Lua): Int {
        val tile = lua.checkUserdata<TransientTileApi>(1)
        val key = lua.checkString(2)
        lua.push(tile.getMetadata(key), Lua.Conversion.FULL)
        return 1
    }

    /**
     * Checks if this tile has a specific tag in its definition.
     *
     * ```signatures
     * HasTag(tag: string) -> boolean
     * ```
     */
    private fun luaHasTag(lua: Lua): Int {
        val tile = lua.checkUserdata<TransientTileApi>(1)
        val tag = lua.checkString(2)
        lua.push(tile.hasTag(tag))
        return 1
    }

    /**
     * Swaps this tile for another tile definition at the same coordinate.
     *
     * ```signatures
     * Swap(newTileDef: TileDefinition) -> TransientTile
     * Swap(newTileDef: TileDefinition, layerName: string) -> TransientTile
     * ```
     */
    private fun luaSwap(lua: Lua): Int {
        val tile = lua.checkUserdata<TransientTileApi>(1)
        val newTileDef = lua.checkRegistry(2, tile.tile.definition.registry)
        val layerName = lua.toString(3)
        val oldTileDef = tile.getDefinition().get()
        if (oldTileDef != null) {
            lua.push(tile.swap(newTileDef, layerName), Lua.Conversion.NONE)
        } else {
            lua.throwError("Tried to swap tile at ${tile.getCoordinate()} but tile definition ${tile.tile.definition.identifier} is not valid")
        }
        return 1
    }

    val luaMeta = LuaMappedMetatable(TransientTileApi::class) {
        getter(::luaGetDefinition)
        getter(::luaGetName)
        getter(::luaGetDimension)
        getter(::luaGetCoordinate)
        getter(::luaGetX)
        getter(::luaGetY)
        getter(::luaGetZ)
        callable(::luaGetMetadata)
        callable(::luaHasTag)
        callable(::luaSwap)
    }

}

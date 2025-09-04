package world.selene.server.maps

import party.iroiro.luajava.Lua
import world.selene.common.data.TileDefinition
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkRegistry
import world.selene.common.lua.checkString
import world.selene.common.lua.checkUserdata
import world.selene.common.util.Coordinate
import world.selene.server.dimensions.Dimension

class TransientTile(
    private val definition: TileDefinition,
    private val dimension: Dimension,
    private val coordinate: Coordinate
) : LuaMetatableProvider {
    val name get() = definition.name
    val x get() = coordinate.x
    val y get() = coordinate.y
    val z get() = coordinate.z

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        /**
         * Gets metadata value for a specific key from this tile's definition.
         * 
         * ```signatures
         * GetMetadata(key: string) -> string|nil
         * ```
         */
        private fun luaGetMetadata(lua: Lua): Int {
            val tile = lua.checkUserdata<TransientTile>(1)
            val key = lua.checkString(2)
            val value = tile.definition.metadata[key]
            lua.push(value, Lua.Conversion.FULL)
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
            val tile = lua.checkUserdata<TransientTile>(1)
            val tag = lua.checkString(2)
            lua.push(tile.definition.tags.contains(tag))
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
            val tile = lua.checkUserdata<TransientTile>(1)
            val newTileDef = lua.checkRegistry(2, tile.definition.registry)
            val layer = lua.toString(3)
            val newTile = tile.dimension.swapTile(tile.coordinate, tile.definition, newTileDef, layer)
            lua.push(newTile, Lua.Conversion.NONE)
            return 1
        }

        val luaMeta = LuaMappedMetatable(TransientTile::class) {
            readOnly(TransientTile::definition)
            readOnly(TransientTile::name)
            readOnly(TransientTile::dimension)
            readOnly(TransientTile::coordinate)
            readOnly(TransientTile::x)
            readOnly(TransientTile::y)
            readOnly(TransientTile::z)
            callable(::luaGetMetadata)
            callable(::luaHasTag)
            callable(::luaSwap)
        }
    }
}
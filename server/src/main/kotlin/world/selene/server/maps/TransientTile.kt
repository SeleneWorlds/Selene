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
         * Gets the definition of this tile.
         *
         * ```property
         * Definition: TileDefinition
         * ```
         */
        private fun luaGetDefinition(lua: Lua): Int {
            val entity = lua.checkUserdata<TransientTile>(1)
            lua.push(entity.definition, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets the name of this tile.
         *
         * ```property
         * Name: string
         * ```
         */
        private fun luaGetName(lua: Lua): Int {
            val entity = lua.checkUserdata<TransientTile>(1)
            lua.push(entity.name)
            return 1
        }

        /**
         * Gets the dimension that this tile belongs to.
         *
         * ```property
         * Dimension: Dimension
         * ```
         */
        private fun luaGetDimension(lua: Lua): Int {
            val entity = lua.checkUserdata<TransientTile>(1)
            lua.push(entity.dimension, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets the coordinate of this tile.
         *
         * ```property
         * Coordinate: Coordinate
         * ```
         */
        private fun luaGetCoordinate(lua: Lua): Int {
            val entity = lua.checkUserdata<TransientTile>(1)
            lua.push(entity.coordinate, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Gets the X coordinate of this tile.
         *
         * ```property
         * X: number
         * ```
         */
        private fun luaGetX(lua: Lua): Int {
            val entity = lua.checkUserdata<TransientTile>(1)
            lua.push(entity.coordinate.x)
            return 1
        }

        /**
         * Gets the Y coordinate of this tile.
         *
         * ```property
         * Y: number
         * ```
         */
        private fun luaGetY(lua: Lua): Int {
            val entity = lua.checkUserdata<TransientTile>(1)
            lua.push(entity.coordinate.y)
            return 1
        }

        /**
         * Gets the Z coordinate of this tile.
         *
         * ```property
         * Z: number
         * ```
         */
        private fun luaGetZ(lua: Lua): Int {
            val entity = lua.checkUserdata<TransientTile>(1)
            lua.push(entity.coordinate.z)
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
}
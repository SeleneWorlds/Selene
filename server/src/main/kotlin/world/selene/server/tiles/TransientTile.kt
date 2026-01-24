package world.selene.server.tiles

import party.iroiro.luajava.Lua
import world.selene.common.data.RegistryReference
import world.selene.common.tiles.TileDefinition
import world.selene.common.lua.*
import world.selene.common.lua.util.checkRegistry
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata
import world.selene.common.grid.Coordinate
import world.selene.common.lua.util.throwError
import world.selene.server.dimensions.Dimension

class TransientTile(
    private val definition: RegistryReference<TileDefinition>,
    private val dimension: Dimension,
    private val coordinate: Coordinate
) : LuaMetatableProvider {
    val identifier get() = definition.identifier
    val x get() = coordinate.x
    val y get() = coordinate.y
    val z get() = coordinate.z

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    @Suppress("SameReturnValue")
    companion object {
        /**
         * Registry definition of this tile.
         *
         * ```property
         * Definition: TileDefinition
         * ```
         */
        private fun luaGetDefinition(lua: Lua): Int {
            val entity = lua.checkUserdata<TransientTile>(1)
            lua.push(entity.definition.get(), Lua.Conversion.NONE)
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
            val entity = lua.checkUserdata<TransientTile>(1)
            lua.push(entity.identifier.toString())
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
            val entity = lua.checkUserdata<TransientTile>(1)
            lua.push(entity.dimension, Lua.Conversion.NONE)
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
            val entity = lua.checkUserdata<TransientTile>(1)
            lua.push(entity.coordinate, Lua.Conversion.NONE)
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
            val entity = lua.checkUserdata<TransientTile>(1)
            lua.push(entity.coordinate.x)
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
            val entity = lua.checkUserdata<TransientTile>(1)
            lua.push(entity.coordinate.y)
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
            val oldTileDef = tile.definition.get()
            if (oldTileDef != null) {
                val newTile = tile.dimension.swapTile(tile.coordinate, oldTileDef, newTileDef, layer)
                lua.push(newTile, Lua.Conversion.NONE)
            } else {
                lua.throwError("Tried to swap tile at ${tile.coordinate} but tile definition ${tile.definition.identifier} is not valid")
            }
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
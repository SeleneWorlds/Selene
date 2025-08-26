package world.selene.server.maps

import party.iroiro.luajava.Lua
import world.selene.common.data.TileDefinition
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkRegistry
import world.selene.common.lua.checkString
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
        val luaMeta = LuaMappedMetatable(TransientTile::class) {
            readOnly(TransientTile::name)
            readOnly(TransientTile::dimension)
            readOnly(TransientTile::coordinate)
            readOnly(TransientTile::x)
            readOnly(TransientTile::y)
            readOnly(TransientTile::z)
            callable("GetMetadata") {
                val tile = it.checkSelf()
                val key = it.checkString(2)
                val value = tile.definition.metadata[key]
                if (value != null) {
                    it.push(value, Lua.Conversion.FULL)
                    return@callable 1
                }
                0
            }
            callable("HasTag") {
                val tile = it.checkSelf()
                val key = it.checkString(2)
                it.push(tile.definition.tags.contains(key))
                1
            }
            callable("Swap") {
                val tile = it.checkSelf()
                val newTileDef = it.checkRegistry(2, tile.definition.registry)
                val layer = it.toString(3)
                val newTile = tile.dimension.swapTile(tile.coordinate, tile.definition, newTileDef, layer)
                it.push(newTile, Lua.Conversion.NONE)
                1
            }
        }
    }
}
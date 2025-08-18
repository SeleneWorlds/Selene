package world.selene.server.maps

import party.iroiro.luajava.Lua
import world.selene.common.data.TileDefinition
import world.selene.common.lua.checkString
import world.selene.common.util.Coordinate
import world.selene.server.dimensions.Dimension

class TileLuaProxy(
    private val name: String,
    private val definition: TileDefinition,
    private val dimension: Dimension.DimensionLuaProxy,
    private val coordinate: Coordinate
) {
    val Name get() = name
    val Dimension get() = dimension
    val Coordinate get() = coordinate
    val X get() = coordinate.x
    val Y get() = coordinate.y
    val Z get() = coordinate.z

    fun GetMetadata(lua: Lua): Int {
        val key = lua.checkString(2)
        val value = definition.metadata[key]
        if (value != null) {
            lua.push(value)
            return 1
        }
        return 0
    }

    fun HasTag(lua: Lua): Int {
        val key = lua.checkString(2)
        lua.push(definition.tags.contains(key))
        return 1
    }
}
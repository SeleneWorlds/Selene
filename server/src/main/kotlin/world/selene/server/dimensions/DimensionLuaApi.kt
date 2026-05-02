package world.selene.server.dimensions

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.util.checkAnyMap
import world.selene.common.lua.util.checkCoordinate
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.checkRegistry
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata
import world.selene.server.cameras.viewer.DefaultViewer
import world.selene.server.cameras.viewer.Viewer
import world.selene.server.maps.tree.MapTreeApi

object DimensionLuaApi {

    private fun luaGetMap(lua: Lua): Int {
        val dimension = lua.checkUserdata<DimensionApi>(1)
        lua.push(dimension.getMapTree(), Lua.Conversion.NONE)
        return 1
    }

    private fun luaSetMap(lua: Lua): Int {
        val dimension = lua.checkUserdata<DimensionApi>(1)
        dimension.setMapTree(lua.checkUserdata<MapTreeApi>(3))
        return 0
    }

    private fun luaHasTile(lua: Lua): Int {
        val dimension = lua.checkUserdata<DimensionApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val tile = lua.checkRegistry(index + 1, dimension.dimension.registries.tiles)
        val viewer = if (lua.isUserdata(index + 2)) lua.checkUserdata<Viewer>(index + 2) else DefaultViewer
        lua.push(dimension.hasTile(coordinate, tile.id, viewer))
        return 1
    }

    private fun luaPlaceTile(lua: Lua): Int {
        val dimension = lua.checkUserdata<DimensionApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val tileDef = lua.checkRegistry(index + 1, dimension.dimension.registries.tiles)
        val layerName = lua.toString(index + 2)
        lua.push(dimension.placeTile(coordinate, tileDef.id, layerName), Lua.Conversion.NONE)
        return 1
    }

    private fun luaAnnotateTile(lua: Lua): Int {
        val dimension = lua.checkUserdata<DimensionApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val key = lua.checkString(index + 1)
        val data = lua.checkAnyMap(index + 2)
        val layerName = lua.toString(index + 3)
        dimension.annotateTile(coordinate, key, data, layerName)
        return 0
    }

    private fun luaGetTilesAt(lua: Lua): Int {
        val dimension = lua.checkUserdata<DimensionApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val viewer = if (lua.isUserdata(index + 1)) lua.checkUserdata<Viewer>(index + 1) else DefaultViewer
        lua.push(dimension.getTilesAt(coordinate, viewer), Lua.Conversion.FULL)
        return 1
    }

    private fun luaGetAnnotationAt(lua: Lua): Int {
        val dimension = lua.checkUserdata<DimensionApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val key = lua.checkString(index + 1)
        val viewer = if (lua.isUserdata(index + 2)) lua.checkUserdata<Viewer>(index + 2) else DefaultViewer
        lua.push(dimension.getAnnotationAt(coordinate, key, viewer), Lua.Conversion.FULL)
        return 1
    }

    private fun luaHasCollisionAt(lua: Lua): Int {
        val dimension = lua.checkUserdata<DimensionApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val viewer = if (lua.isUserdata(index + 1)) lua.checkUserdata<Viewer>(index + 1) else DefaultViewer
        lua.push(dimension.hasCollisionAt(coordinate, viewer))
        return 1
    }

    private fun luaGetEntitiesAt(lua: Lua): Int {
        val dimension = lua.checkUserdata<DimensionApi>(1)
        val (coordinate, _) = lua.checkCoordinate(2)
        lua.push(dimension.getEntitiesAt(coordinate), Lua.Conversion.FULL)
        return 1
    }

    private fun luaGetEntitiesInRange(lua: Lua): Int {
        val dimension = lua.checkUserdata<DimensionApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val range = lua.checkInt(index + 1)
        lua.push(dimension.getEntitiesInRange(coordinate, range), Lua.Conversion.FULL)
        return 1
    }

    val luaMeta = LuaMappedMetatable(DimensionApi::class) {
        getter(::luaGetMap)
        setter(::luaSetMap)
        callable(::luaHasTile)
        callable(::luaPlaceTile)
        callable(::luaAnnotateTile)
        callable(::luaGetTilesAt)
        callable(::luaGetAnnotationAt)
        callable(::luaHasCollisionAt)
        callable(::luaGetEntitiesAt)
        callable(::luaGetEntitiesInRange)
    }

}

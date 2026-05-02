package com.seleneworlds.server.dimensions

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkCoordinate
import com.seleneworlds.common.lua.util.checkInt
import com.seleneworlds.common.lua.util.checkRegistry
import com.seleneworlds.common.lua.util.checkSerializedMap
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.server.cameras.viewer.DefaultViewer
import com.seleneworlds.server.cameras.viewer.Viewer
import com.seleneworlds.server.maps.tree.MapTreeApi

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
        val data = lua.checkSerializedMap(index + 2)
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

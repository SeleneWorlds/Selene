package com.seleneworlds.server.maps.tree

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkAnyMap
import com.seleneworlds.common.lua.util.checkBoolean
import com.seleneworlds.common.lua.util.checkCoordinate
import com.seleneworlds.common.lua.util.checkRegistry
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata

object MapTreeLuaApi {

    /**
     * Merges another MapTree into this one, copying all tiles and annotations.
     *
     * ```signatures
     * Merge(other: MapTree)
     * ```
     */
    private fun luaMerge(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val other = lua.checkUserdata<MapTreeApi>(2)
        mapTree.merge(other)
        return 0
    }

    /**
     * Places a tile at the specified coordinate on the given layer.
     *
     * ```signatures
     * PlaceTile(coordinate: Coordinate, tileDef: TileDefinition)
     * PlaceTile(coordinate: Coordinate, tileDef: TileDefinition, layerName: string)
     * ```
     */
    private fun luaPlaceTile(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val tileDef = lua.checkRegistry(index + 1, mapTree.mapTree.registries.tiles)
        val layerName = lua.toString(index + 2)
        mapTree.placeTile(coordinate, tileDef, layerName)
        return 0
    }

    /**
     * Replaces all tiles at the specified coordinate with the given tile.
     *
     * ```signatures
     * ReplaceTiles(coordinate: Coordinate, tileDef: TileDefinition)
     * ReplaceTiles(coordinate: Coordinate, tileDef: TileDefinition, layerName: string)
     * ```
     */
    private fun luaReplaceTiles(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val tileDef = lua.checkRegistry(index + 1, mapTree.mapTree.registries.tiles)
        val layerName = lua.toString(index + 2)
        mapTree.replaceTiles(coordinate, tileDef, layerName)
        return 0
    }

    /**
     * Swaps one tile for another at the specified coordinate.
     *
     * ```signatures
     * SwapTile(coordinate: Coordinate, oldTileDef: TileDefinition, newTileDef: TileDefinition)
     * SwapTile(coordinate: Coordinate, oldTileDef: TileDefinition, newTileDef: TileDefinition, layerName: string)
     * ```
     */
    private fun luaSwapTile(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val oldTileDef = lua.checkRegistry(index + 1, mapTree.mapTree.registries.tiles)
        val newTileDef = lua.checkRegistry(index + 2, mapTree.mapTree.registries.tiles)
        val layerName = lua.toString(index + 3)
        mapTree.swapTile(coordinate, oldTileDef, newTileDef, layerName)
        return 0
    }

    /**
     * Removes a specific tile from the specified coordinate.
     *
     * ```signatures
     * RemoveTile(coordinate: Coordinate, tileDef: TileDefinition)
     * RemoveTile(coordinate: Coordinate, tileDef: TileDefinition, layerName: string)
     * ```
     */
    private fun luaRemoveTile(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val tileDef = lua.checkRegistry(index + 1, mapTree.mapTree.registries.tiles)
        val layerName = lua.toString(index + 2)
        mapTree.removeTile(coordinate, tileDef, layerName)
        return 0
    }

    /**
     * Removes all tiles at the specified coordinate.
     *
     * ```signatures
     * ResetTile(coordinate: Coordinate)
     * ResetTile(coordinate: Coordinate, layerName: string)
     * ```
     */
    private fun luaResetTile(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val layerName = lua.toString(index + 1)
        mapTree.resetTile(coordinate, layerName)
        return 0
    }

    /**
     * Adds annotation data to a tile at the specified coordinate.
     *
     * ```signatures
     * AnnotateTile(coordinate: Coordinate, key: string, data: table)
     * AnnotateTile(coordinate: Coordinate, key: string, data: table, layerName: string)
     * ```
     */
    private fun luaAnnotateTile(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val (coordinate, index) = lua.checkCoordinate(2)
        val key = lua.checkString(index + 1)
        val data = lua.checkAnyMap(index + 2)
        val layerName = lua.toString(index + 3)
        mapTree.annotateTile(coordinate, key, data, layerName)
        return 0
    }

    /**
     * Sets the visibility of a layer for a specific vision tag.
     *
     * ```signatures
     * SetVisibility(layerName: string, enabled: boolean)
     * SetVisibility(layerName: string, enabled: boolean, tag: string)
     * ```
     */
    private fun luaSetVisibility(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val layerName = lua.checkString(2)
        val enabled = lua.checkBoolean(3)
        val tagName = if (lua.isString(4)) lua.checkString(4) else "default"
        mapTree.setVisibility(layerName, enabled, tagName)
        return 0
    }

    /**
     * Checks if a layer is visible for a specific vision tag.
     *
     * ```signatures
     * IsVisible(layerName: string) -> boolean
     * IsVisible(layerName: string, tag: string) -> boolean
     * ```
     */
    private fun luaIsVisible(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val layerName = lua.checkString(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        lua.push(mapTree.isVisible(layerName, tagName))
        return 1
    }

    /**
     * Checks if a layer is invisible for a specific vision tag.
     *
     * ```signatures
     * IsInvisible(layerName: string) -> boolean
     * IsInvisible(layerName: string, tag: string) -> boolean
     * ```
     */
    private fun luaIsInvisible(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val layerName = lua.checkString(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        lua.push(mapTree.isInvisible(layerName, tagName))
        return 1
    }

    /**
     * Makes a layer visible for a specific vision tag.
     *
     * ```signatures
     * MakeVisible(layerName: string)
     * MakeVisible(layerName: string, tag: string)
     * ```
     */
    private fun luaMakeVisible(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val layerName = lua.checkString(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        mapTree.makeVisible(layerName, tagName)
        return 0
    }

    /**
     * Makes a layer invisible for a specific vision tag.
     *
     * ```signatures
     * MakeInvisible(layerName: string)
     * MakeInvisible(layerName: string, tag: string)
     * ```
     */
    private fun luaMakeInvisible(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val layerName = lua.checkString(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        mapTree.makeInvisible(layerName, tagName)
        return 0
    }

    /**
     * Checks if a layer has collision enabled for a specific tag.
     *
     * ```signatures
     * HasCollisions(layerName: string) -> boolean
     * HasCollisions(layerName: string, tag: string) -> boolean
     * ```
     */
    private fun luaHasCollisions(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val layerName = lua.checkString(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        lua.push(mapTree.hasCollisions(layerName, tagName))
        return 1
    }

    /**
     * Sets collision state for a layer with a specific tag.
     *
     * ```signatures
     * SetCollisions(layerName: string, enabled: boolean)
     * SetCollisions(layerName: string, enabled: boolean, tag: string)
     * ```
     */
    private fun luaSetCollisions(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val layerName = lua.checkString(2)
        val enabled = lua.checkBoolean(3)
        val tagName = if (lua.isString(4)) lua.checkString(4) else "default"
        mapTree.setCollisions(layerName, enabled, tagName)
        return 0
    }

    /**
     * Enables collision for a layer with a specific tag.
     *
     * ```signatures
     * EnableCollisions(layerName: string)
     * EnableCollisions(layerName: string, tag: string)
     * ```
     */
    private fun luaEnableCollisions(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val layerName = lua.checkString(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        mapTree.enableCollisions(layerName, tagName)
        return 0
    }

    /**
     * Disables collision for a layer with a specific tag.
     *
     * ```signatures
     * DisableCollisions(layerName: string)
     * DisableCollisions(layerName: string, tag: string)
     * ```
     */
    private fun luaDisableCollisions(lua: Lua): Int {
        val mapTree = lua.checkUserdata<MapTreeApi>(1)
        val layerName = lua.checkString(2)
        val tagName = if (lua.isString(3)) lua.checkString(3) else "default"
        mapTree.disableCollisions(layerName, tagName)
        return 0
    }

    val luaMeta = LuaMappedMetatable(MapTreeApi::class) {
        callable(::luaMerge)
        callable(::luaPlaceTile)
        callable(::luaReplaceTiles)
        callable(::luaSwapTile)
        callable(::luaRemoveTile)
        callable(::luaResetTile)
        callable(::luaAnnotateTile)
        callable(::luaSetVisibility)
        callable(::luaMakeVisible)
        callable(::luaMakeInvisible)
        callable(::luaIsVisible)
        callable(::luaIsInvisible)
        callable(::luaSetCollisions)
        callable(::luaEnableCollisions)
        callable(::luaDisableCollisions)
        callable(::luaHasCollisions)
    }

}

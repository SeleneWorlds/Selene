package com.seleneworlds.client.tiles

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkUserdata

object TileLuaApi {

    /**
     * Coordinate this tile is located at.
     *
     * ```property
     * Coordinate: Coordinate
     * ```
     */
    private fun getCoordinate(lua: Lua): Int {
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
    private fun getDefinition(lua: Lua): Int {
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
    private fun getVisual(lua: Lua): Int {
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
    private fun getX(lua: Lua): Int {
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
    private fun getY(lua: Lua): Int {
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
    private fun getZ(lua: Lua): Int {
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
    private fun getName(lua: Lua): Int {
        val tile = lua.checkUserdata<TileApi>(1)
        lua.push(tile.getName())
        return 1
    }

    val luaMeta = LuaMappedMetatable(TileApi::class) {
        callable(::getCoordinate)
        callable(::getDefinition)
        callable(::getVisual)
        callable(::getX)
        callable(::getY)
        callable(::getZ)
        callable(::getName)
    }
}

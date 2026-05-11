package com.seleneworlds.client.maps

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.client.game.ClientEvents
import com.seleneworlds.client.tiles.TileApi
import com.seleneworlds.client.tiles.TileLuaApi
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkCoordinate
import com.seleneworlds.common.script.ScriptTrace
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.xpCall

/**
 * Lookup tiles on the map.
 */
class MapLuaApi(
    private val api: MapApi
) : LuaModule {
    override val name = "selene.map"

    override fun initialize(luaManager: LuaManager) {
        luaManager.defineMetatable(TileApi::class, TileLuaApi.luaMeta)
    }

    private val mapChunkChanged = LuaEventSink(ClientEvents.MapChunkChanged.EVENT) { callback: LuaValue, trace: ScriptTrace ->
        ClientEvents.MapChunkChanged { coordinate, width, height ->
            val lua = callback.state()
            lua.push(callback)
            lua.push(coordinate, Lua.Conversion.NONE)
            lua.push(width)
            lua.push(height)
            lua.xpCall(3, 0, trace)
        }
    }

    override fun register(table: LuaValue) {
        table.register("getTilesAt", this::getTilesAt)
        table.register("hasTileAt", this::hasTileAt)
        table.set("onChunkChanged", mapChunkChanged)
    }

    private fun getTilesAt(lua: Lua): Int {
        val (coordinate, _) = lua.checkCoordinate(1)
        lua.push(api.getTilesAt(coordinate.x, coordinate.y, coordinate.z), Lua.Conversion.FULL)
        return 1
    }

    private fun hasTileAt(lua: Lua): Int {
        val (coordinate, _) = lua.checkCoordinate(1)
        lua.push(api.hasTileAt(coordinate.x, coordinate.y, coordinate.z))
        return 1
    }
}

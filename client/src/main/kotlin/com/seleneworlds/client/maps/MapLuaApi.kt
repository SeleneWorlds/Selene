package com.seleneworlds.client.maps

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.client.game.ClientEvents
import com.seleneworlds.client.tiles.TileApi
import com.seleneworlds.client.tiles.TileLuaApi
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.script.ScriptTrace
import com.seleneworlds.common.lua.util.checkInt
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
        table.register("GetTilesAt", this::luaGetTilesAt)
        table.register("HasTileAt", this::luaHasTileAt)
        table.set("OnChunkChanged", mapChunkChanged)
    }

    private fun luaGetTilesAt(lua: Lua): Int {
        lua.push(api.getTilesAt(lua.checkInt(1), lua.checkInt(2), lua.checkInt(3)), Lua.Conversion.FULL)
        return 1
    }

    private fun luaHasTileAt(lua: Lua): Int {
        lua.push(api.hasTileAt(lua.checkInt(1), lua.checkInt(2), lua.checkInt(3)))
        return 1
    }
}

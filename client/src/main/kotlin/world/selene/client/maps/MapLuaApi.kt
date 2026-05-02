package world.selene.client.maps

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.game.ClientEvents
import world.selene.client.tiles.TileApi
import world.selene.client.tiles.TileLuaApi
import world.selene.common.lua.LuaEventSink
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.script.ScriptTrace
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.register
import world.selene.common.lua.util.xpCall

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

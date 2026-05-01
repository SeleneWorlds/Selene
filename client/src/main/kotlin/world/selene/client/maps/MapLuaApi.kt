package world.selene.client.maps

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.lua.ClientLuaSignals
import world.selene.common.lua.LuaModule
import world.selene.common.lua.Signal
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.register

/**
 * Lookup tiles on the map.
 */
@Suppress("SameReturnValue")
class MapLuaApi(
    private val api: MapApi,
    signals: ClientLuaSignals
) : LuaModule {
    override val name = "selene.map"

    private val mapChunkChanged: Signal = signals.mapChunkChanged

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

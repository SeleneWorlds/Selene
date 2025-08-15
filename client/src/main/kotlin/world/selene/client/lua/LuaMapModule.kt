package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.maps.ClientMap
import world.selene.client.maps.Tile
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkInt
import world.selene.common.lua.register
import world.selene.common.util.Coordinate

class LuaMapModule(
    private val clientMap: ClientMap,
    private val signals: ClientLuaSignals
) : LuaModule {
    override val name = "selene.map"

    override fun initialize(luaManager: LuaManager) {
        luaManager.exposeClass(Tile.TileLuaProxy::class)
    }

    override fun register(table: LuaValue) {
        table.register("GetTilesAt", this::luaGetTilesAt)
        table.register("HasTileAt", this::luaHasTileAt)
        table.set("OnChunkChanged", signals.mapChunkChanged)
    }

    private fun luaGetTilesAt(lua: Lua): Int {
        val x = lua.checkInt(1)
        val y = lua.checkInt(2)
        val z = lua.checkInt(3)
        val coordinate = Coordinate(x, y, z)

        try {
            val tiles = clientMap.getTilesAt(coordinate)
            val tileProxies = tiles.map { it.luaProxy }
            lua.push(tileProxies, Lua.Conversion.FULL)
            return 1
        } catch (e: Exception) {
            return lua.error(RuntimeException("Failed to get tiles at ($x, $y, $z): ${e.message}", e))
        }
    }

    private fun luaHasTileAt(lua: Lua): Int {
        val x = lua.checkInt(1)
        val y = lua.checkInt(2)
        val z = lua.checkInt(3)
        val coordinate = Coordinate(x, y, z)

        try {
            val hasTile = clientMap.hasTileAt(coordinate)
            lua.push(hasTile)
            return 1
        } catch (e: Exception) {
            return lua.error(RuntimeException("Failed to check tile at ($x, $y, $z): ${e.message}", e))
        }
    }

}

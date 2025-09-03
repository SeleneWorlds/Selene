package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.maps.ClientMap
import world.selene.client.maps.Tile
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.Signal
import world.selene.common.lua.checkInt
import world.selene.common.lua.register
import world.selene.common.util.Coordinate

/**
 * Provides functions for accessing tiles on the map.
 */
class LuaClientMapModule(
    private val clientMap: ClientMap,
    private val signals: ClientLuaSignals
) : LuaModule {
    override val name = "selene.map"

    /**
     * Fired when tiles inside a map chunk changed.
     */
    private val mapChunkChanged: Signal = signals.mapChunkChanged

    override fun register(table: LuaValue) {
        table.register("GetTilesAt", this::luaGetTilesAt)
        table.register("HasTileAt", this::luaHasTileAt)
        table.set("OnChunkChanged", mapChunkChanged)
    }

    /**
     * Gets all tiles at the specified coordinate.
     *
     * ```signatures
     * GetTilesAt(x: number, y: number, z: number) -> table[Tile]
     * ```
     */
    private fun luaGetTilesAt(lua: Lua): Int {
        val x = lua.checkInt(1)
        val y = lua.checkInt(2)
        val z = lua.checkInt(3)
        val coordinate = Coordinate(x, y, z)

        val tiles = clientMap.getTilesAt(coordinate)
        lua.push(tiles, Lua.Conversion.FULL)
        return 1
    }

    /**
     * Checks if any tiles exist at the specified coordinate.
     *
     * ```signatures
     * HasTileAt(x: number, y: number, z: number) -> boolean
     * ```
     */
    private fun luaHasTileAt(lua: Lua): Int {
        val x = lua.checkInt(1)
        val y = lua.checkInt(2)
        val z = lua.checkInt(3)
        val coordinate = Coordinate(x, y, z)

        val hasTile = clientMap.hasTileAt(coordinate)
        lua.push(hasTile)
        return 1
    }

}

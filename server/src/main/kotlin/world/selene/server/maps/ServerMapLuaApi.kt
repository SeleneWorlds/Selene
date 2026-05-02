package world.selene.server.maps

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register
import world.selene.server.maps.tree.MapTreeApi
import world.selene.server.maps.tree.MapTreeLuaApi
import world.selene.server.sync.ScopedChunkViewApi
import world.selene.server.sync.ScopedChunkViewLuaApi
import world.selene.server.tiles.TransientTileApi
import world.selene.server.tiles.TransientTileLuaApi

/**
 * Create new map layer trees.
 */
class ServerMapLuaApi(private val api: ServerMapApi) : LuaModule {
    override val name = "selene.map"

    override fun initialize(luaManager: LuaManager) {
        luaManager.defineMetatable(MapTreeApi::class, MapTreeLuaApi.luaMeta)
        luaManager.defineMetatable(ScopedChunkViewApi::class, ScopedChunkViewLuaApi.luaMeta)
        luaManager.defineMetatable(TransientTileApi::class, TransientTileLuaApi.luaMeta)
    }

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreate)
    }

    private fun luaCreate(lua: Lua): Int {
        lua.push(api.create(), Lua.Conversion.NONE)
        return 1
    }
}

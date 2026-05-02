package com.seleneworlds.server.maps

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.server.maps.tree.MapTreeApi
import com.seleneworlds.server.maps.tree.MapTreeLuaApi
import com.seleneworlds.server.sync.ScopedChunkViewApi
import com.seleneworlds.server.sync.ScopedChunkViewLuaApi
import com.seleneworlds.server.tiles.TransientTileApi
import com.seleneworlds.server.tiles.TransientTileLuaApi

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

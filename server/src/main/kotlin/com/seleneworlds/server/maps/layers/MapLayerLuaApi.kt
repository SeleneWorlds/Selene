package com.seleneworlds.server.maps.layers

import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkUserdata
import party.iroiro.luajava.Lua

object MapLayerLuaApi {
    private fun getName(lua: Lua): Int {
        lua.push(lua.checkUserdata<MapLayerApi>(1).getName())
        return 1
    }

    val luaMeta = LuaMappedMetatable(MapLayerApi::class) {
        callable(::getName)
    }
}

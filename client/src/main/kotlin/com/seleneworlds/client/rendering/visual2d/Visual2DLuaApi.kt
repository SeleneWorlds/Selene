package com.seleneworlds.client.rendering.visual2d

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata

object Visual2DLuaApi {

    /**
     * Gets metadata value for the specified key from this visual.
     *
     * ```signatures
     * GetMetadata(key: string) -> any|nil
     * ```
     */
    private fun luaGetMetadata(lua: Lua): Int {
        val self = lua.checkUserdata<Visual2DApi>(1)
        val key = lua.checkString(2)
        lua.push(self.getMetadata(key), Lua.Conversion.FULL)
        return 1
    }

    val luaMeta = LuaMappedMetatable(Visual2DApi::class) {
        callable(::luaGetMetadata)
    }
}

package com.seleneworlds.server.dimensions

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.register

/**
 * Create or lookup dimensions.
 */
class DimensionsLuaApi(private val api: DimensionsApi) : LuaModule {
    override val name = "selene.dimensions"

    override fun initialize(luaManager: LuaManager) {
        luaManager.defineMetatable(DimensionApi::class, DimensionLuaApi.luaMeta)
    }

    override fun register(table: LuaValue) {
        table.register("GetDefault", this::luaGetDefault)
    }

    /**
     * Returns the default dimension (dimension 0), which is always available.
     *
     * ```signatures
     * GetDefault() -> Dimension
     * ```
     */
    private fun luaGetDefault(lua: Lua): Int {
        lua.push(api.getDefault(), Lua.Conversion.NONE)
        return 1
    }
}

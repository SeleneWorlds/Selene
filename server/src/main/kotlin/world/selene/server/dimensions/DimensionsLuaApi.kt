package world.selene.server.dimensions

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register

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

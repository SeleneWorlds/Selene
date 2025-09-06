package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.register
import world.selene.server.dimensions.DimensionManager

/**
 * Create or lookup dimensions.
 */
@Suppress("SameReturnValue")
class LuaDimensionsModule(private val dimensionManager: DimensionManager) : LuaModule {
    override val name = "selene.dimensions"

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
        lua.push(dimensionManager.getOrCreateDimension(0), Lua.Conversion.NONE)
        return 1
    }
}

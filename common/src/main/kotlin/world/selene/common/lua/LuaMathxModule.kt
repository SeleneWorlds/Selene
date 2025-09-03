package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

/**
 * Provides extended mathematical functions beyond standard Lua math library.
 * Registered as global "mathx" table.
 */
class LuaMathxModule : LuaModule {
    override val name: String = "mathx"
    override val registerAsGlobal: Boolean = true

    override fun register(table: LuaValue) {
        table.register("clamp", this::luaClamp)
    }

    /**
     * Clamps a value between minimum and maximum bounds.
     * Works with both integers and floating-point numbers.
     *
     * ```lua
     * number clamp(number value, number min, number max)
     * ```
     */
    private fun luaClamp(lua: Lua): Int {
        if (lua.isInteger(1) && lua.isInteger(2) && lua.isInteger(3)) {
            lua.push(lua.checkInt(1).coerceIn(lua.checkInt(2), lua.checkInt(3)))
        } else {
            lua.push(lua.checkFloat(1).coerceIn(lua.checkFloat(2), lua.checkFloat(3)))
        }
        return 1
    }

}
package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkString
import world.selene.common.lua.checkType
import world.selene.common.lua.register
import world.selene.server.config.ScriptProperties

/**
 * Provides access to properties configured in the `script.properties` file.
 */
class LuaConfigModule(private val scriptProperties: ScriptProperties) : LuaModule {
    override val name = "selene.config"

    override fun register(table: LuaValue) {
        table.register("GetProperty", this::luaGetProperty)
    }

    /**
     * Retrieves a configuration property value by key.
     * If the property is not found, returns the default value or nil.
     *
     * ```lua
     * string|nil GetProperty(string key)
     * string GetProperty(string key, string defaultValue)
     * ```
     */
    private fun luaGetProperty(lua: Lua): Int {
        val key = lua.checkString(1)
        if (lua.top >= 2) lua.checkType(2, Lua.LuaType.STRING)

        val value = scriptProperties.getProperty(key)
        if (value != null) {
            lua.push(value)
        } else {
            lua.pushValue(2)
        }
        return 1
    }
}

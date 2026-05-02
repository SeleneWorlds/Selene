package world.selene.common.i18n

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.register
import world.selene.common.lua.util.toLocale
import world.selene.common.lua.util.toTypedMap

/**
 * Localize messages.
 */
class I18nLuaApi(private val api: I18nApi) : LuaModule {
    override val name = "selene.i18n"

    override fun register(table: LuaValue) {
        table.register("Get", this::luaGet)
        table.register("Format", this::luaFormat)
        table.register("HasKey", this::luaHasKey)
    }

    private fun luaGet(lua: Lua): Int {
        val value = api.get(lua.checkString(1), lua.toLocale(2))
        if (value != null) {
            lua.push(value)
        } else {
            lua.pushNil()
        }
        return 1
    }

    private fun luaFormat(lua: Lua): Int {
        val value = api.format(lua.checkString(1), lua.toTypedMap<String, Any>(2) ?: emptyMap(), lua.toLocale(3))
        if (value != null) {
            lua.push(value)
        } else {
            lua.pushNil()
        }
        return 1
    }

    private fun luaHasKey(lua: Lua): Int {
        lua.push(api.hasKey(lua.checkString(1), lua.toLocale(2)))
        return 1
    }
}

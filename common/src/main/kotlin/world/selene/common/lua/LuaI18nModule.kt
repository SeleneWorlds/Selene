package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.i18n.Messages

class LuaI18nModule(
    private val messages: Messages
) : LuaModule {
    override val name = "selene.i18n"

    override fun register(table: LuaValue) {
        table.register("Get", this::luaGet)
        table.register("Format", this::luaFormat)
        table.register("HasKey", this::luaHasKey)
    }

    private fun luaGet(lua: Lua): Int {
        val key = lua.checkString(1)
        val locale = lua.toLocale(2)
        val value = messages.get(key, locale)
        if (value != null) {
            lua.push(value)
        } else {
            lua.pushNil()
        }
        return 1
    }

    private fun luaFormat(lua: Lua): Int {
        val key = lua.checkString(1)
        val args = lua.toTypedMap<String, Any>(2) ?: emptyMap()
        val locale = lua.toLocale(3)
        val value = messages.format(key, args, locale)
        if (value != null) {
            lua.push(value)
        } else {
            lua.pushNil()
        }
        return 1
    }

    private fun luaHasKey(lua: Lua): Int {
        val key = lua.checkString(1)
        val locale = lua.toLocale(2)
        lua.push(messages.has(key, locale))
        return 1
    }

}

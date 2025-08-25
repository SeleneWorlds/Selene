package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.i18n.Messages
import java.util.*

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
        val locale = if (lua.top >= 2) lua.checkString(2).let { Locale.forLanguageTag(it) } else null
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
        @Suppress("UNCHECKED_CAST") val args = if (!lua.isNil(2)) lua.toMap(2) as Map<String, Any> else emptyMap()
        val locale = if (lua.top >= 3) lua.checkString(3).let { Locale.forLanguageTag(it) } else null
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
        val locale = if (lua.top >= 2) lua.checkString(2).let { Locale.forLanguageTag(it) } else null
        lua.push(messages.has(key, locale))
        return 1
    }

}

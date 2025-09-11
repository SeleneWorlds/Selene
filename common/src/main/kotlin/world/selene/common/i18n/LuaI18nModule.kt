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
@Suppress("SameReturnValue")
class LuaI18nModule(private val messages: Messages) : LuaModule {
    override val name = "selene.i18n"

    override fun register(table: LuaValue) {
        table.register("Get", this::luaGet)
        table.register("Format", this::luaFormat)
        table.register("HasKey", this::luaHasKey)
    }

    /**
     * Retrieves a message for the given locale.
     * If no locale is given, the default locale will be used.
     * If the key is not defined, this function will return `nil`.
     *
     * ```signatures
     * Get(key: string) -> string|nil
     * Get(key: string, locale: Locale) -> string|nil
     * ```
     */
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

    /**
     * Retrieves a message for the given locale and replaces its placeholders with the given arguments.
     * If no locale is given, the default locale will be used.
     * If the key is not defined, this function will return `nil`.
     *
     * ```signatures
     * Format(key: string) -> string|nil
     * Format(key: string, args: args) -> string|nil
     * Format(key: string, args: table|nil, locale: Locale) -> string|nil
     * ```
     */
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

    /**
     * Checks whether a key is defined in a localization bundle for the given locale.
     * If no locale is given, the default locale will be used.
     *
     * ```signatures
     * HasKey(key: string) -> boolean
     * HasKey(key: string, locale: Locale) -> boolean
     * ```
     */
    private fun luaHasKey(lua: Lua): Int {
        val key = lua.checkString(1)
        val locale = lua.toLocale(2)
        lua.push(messages.has(key, locale))
        return 1
    }

}

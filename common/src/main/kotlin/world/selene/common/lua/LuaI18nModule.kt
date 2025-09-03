package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.i18n.Messages

/**
 * Provides access to localization messages within bundles.
 */
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
     * ```lua
     * string|nil Get(string key)
     * string|nil Get(string key, Locale locale)
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
     * ```lua
     * string|nil Format(string key)
     * string|nil Format(string key, table args)
     * string|nil Format(string key, table|nil args, Locale locale)
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
     * ```lua
     * boolean HasKey(string key)
     * boolean HasKey(string key, Locale locale)
     * ```
     */
    private fun luaHasKey(lua: Lua): Int {
        val key = lua.checkString(1)
        val locale = lua.toLocale(2)
        lua.push(messages.has(key, locale))
        return 1
    }

}

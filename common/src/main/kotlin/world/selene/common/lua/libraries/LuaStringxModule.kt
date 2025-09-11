package world.selene.common.lua.libraries

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.register

/**
 * Extended string manipulation functions beyond standard Lua string library.
 * Registered as `stringx` global.
 */
@Suppress("SameReturnValue")
class LuaStringxModule : LuaModule {
    override val name: String = "stringx"
    override val registerAsGlobal: Boolean = true

    override fun register(table: LuaValue) {
        table.register("trim", this::luaTrim)
        table.register("startsWith", this::luaStartsWith)
        table.register("endsWith", this::luaEndsWith)
        table.register("removePrefix", this::luaRemovePrefix)
        table.register("removeSuffix", this::luaRemoveSuffix)
        table.register("split", this::luaSplit)
        table.register("substringBefore", this::luaSubstringBefore)
        table.register("substringAfter", this::luaSubstringAfter)
    }

    /**
     * Checks if a string starts with the specified prefix.
     *
     * ```signatures
     * startsWith(str: string, prefix: string) -> boolean
     * ```
     */
    private fun luaStartsWith(lua: Lua): Int {
        lua.push(lua.checkString(1).startsWith(lua.checkString(2)))
        return 1
    }

    /**
     * Checks if a string ends with the specified suffix.
     *
     * ```signatures
     * endsWith(str: string, suffix: string) -> boolean
     * ```
     */
    private fun luaEndsWith(lua: Lua): Int {
        lua.push(lua.checkString(1).endsWith(lua.checkString(2)))
        return 1
    }

    /**
     * Removes the specified prefix from a string if it exists.
     *
     * ```signatures
     * removePrefix(str: string, prefix: string) -> string
     * ```
     */
    private fun luaRemovePrefix(lua: Lua): Int {
        lua.push(lua.checkString(1).removePrefix(lua.checkString(2)))
        return 1
    }

    /**
     * Removes the specified suffix from a string if it exists.
     *
     * ```signatures
     * removeSuffix(str: string, suffix: string) -> string
     * ```
     */
    private fun luaRemoveSuffix(lua: Lua): Int {
        lua.push(lua.checkString(1).removeSuffix(lua.checkString(2)))
        return 1
    }

    /**
     * Removes leading and trailing whitespace from a string.
     *
     * ```signatures
     * trim(str: string) -> string
     * ```
     */
    private fun luaTrim(lua: Lua): Int {
        lua.push(lua.checkString(1).trim())
        return 1
    }

    /**
     * Returns the substring before the first occurrence of the separator.
     *
     * ```signatures
     * substringBefore(str: string, separator: string) -> string
     * ```
     */
    private fun luaSubstringBefore(lua: Lua): Int {
        val str = lua.checkString(1)
        val separator = lua.checkString(2)
        val result = str.substringBefore(separator)
        lua.push(result)
        return 1
    }

    /**
     * Returns the substring after the first occurrence of the separator.
     *
     * ```signatures
     * substringAfter(str: string, separator: string) -> string
     * ```
     */
    private fun luaSubstringAfter(lua: Lua): Int {
        val str = lua.checkString(1)
        val separator = lua.checkString(2)
        val result = str.substringAfter(separator)
        lua.push(result)
        return 1
    }

    /**
     * Splits a string into a table of substrings using the specified separator.
     *
     * ```signatures
     * split(str: string, separator: string) -> table
     * ```
     */
    private fun luaSplit(lua: Lua): Int {
        val str = lua.checkString(1)
        val separator = lua.checkString(2)
        val result = str.split(separator)
        lua.push(result, Lua.Conversion.FULL)
        return 1
    }

}
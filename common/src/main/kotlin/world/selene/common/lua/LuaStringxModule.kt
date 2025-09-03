package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

/**
 * Provides extended string manipulation functions beyond standard Lua string library.
 * Registered as `stringx` global.
 */
class LuaStringxModule : LuaModule {
    override val name: String = "stringx"
    override val registerAsGlobal: Boolean = true

    override fun register(table: LuaValue) {
        table.register("trim", this::luaTrim)
        table.register("startsWith", this::luaStartsWith)
        table.register("endsWith", this::luaEndsWith)
        table.register("removeSuffix", this::luaRemoveSuffix)
        table.register("split", this::luaSplit)
        table.register("substringAfter", this::luaSubstringAfter)
    }

    /**
     * Checks if a string starts with the specified prefix.
     *
     * ```lua
     * boolean startsWith(string str, string prefix)
     * ```
     */
    private fun luaStartsWith(lua: Lua): Int {
        lua.push(lua.checkString(1).startsWith(lua.checkString(2)))
        return 1
    }

    /**
     * Checks if a string ends with the specified suffix.
     *
     * ```lua
     * boolean endsWith(string str, string suffix)
     * ```
     */
    private fun luaEndsWith(lua: Lua): Int {
        lua.push(lua.checkString(1).endsWith(lua.checkString(2)))
        return 1
    }

    /**
     * Removes the specified suffix from a string if it exists.
     *
     * ```lua
     * string removeSuffix(string str, string suffix)
     * ```
     */
    private fun luaRemoveSuffix(lua: Lua): Int {
        lua.push(lua.checkString(1).removeSuffix(lua.checkString(2)))
        return 1
    }

    /**
     * Removes leading and trailing whitespace from a string.
     *
     * ```lua
     * string trim(string str)
     * ```
     */
    private fun luaTrim(lua: Lua): Int {
        lua.push(lua.checkString(1).trim())
        return 1
    }

    /**
     * Returns the substring after the first occurrence of the separator.
     *
     * ```lua
     * string substringAfter(string str, string separator)
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
     * ```lua
     * table split(string str, string separator)
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
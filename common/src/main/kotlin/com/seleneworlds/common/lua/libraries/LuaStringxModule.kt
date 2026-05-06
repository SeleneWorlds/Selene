package com.seleneworlds.common.lua.libraries

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register

/**
 * Extended string manipulation functions beyond standard Lua string library.
 * Registered as `stringx` global.
 */
class LuaStringxModule : LuaModule {
    override val name: String = "stringx"
    override val registerAsGlobal: Boolean = true

    override fun register(table: LuaValue) {
        table.register("trim", this::trim)
        table.register("startsWith", this::startsWith)
        table.register("endsWith", this::endsWith)
        table.register("removePrefix", this::removePrefix)
        table.register("removeSuffix", this::removeSuffix)
        table.register("split", this::split)
        table.register("substringBefore", this::substringBefore)
        table.register("substringAfter", this::substringAfter)
    }

    /**
     * Checks if a string starts with the specified prefix.
     *
     * ```signatures
     * startsWith(str: string, prefix: string) -> boolean
     * ```
     */
    private fun startsWith(lua: Lua): Int {
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
    private fun endsWith(lua: Lua): Int {
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
    private fun removePrefix(lua: Lua): Int {
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
    private fun removeSuffix(lua: Lua): Int {
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
    private fun trim(lua: Lua): Int {
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
    private fun substringBefore(lua: Lua): Int {
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
    private fun substringAfter(lua: Lua): Int {
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
    private fun split(lua: Lua): Int {
        val str = lua.checkString(1)
        val separator = lua.checkString(2)
        val result = str.split(separator)
        lua.push(result, Lua.Conversion.FULL)
        return 1
    }

}
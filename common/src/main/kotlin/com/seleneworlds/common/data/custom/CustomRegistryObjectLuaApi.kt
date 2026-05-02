package com.seleneworlds.common.data.custom

import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import com.seleneworlds.common.serialization.unwrap

object CustomRegistryObjectLuaApi {
    /**
     * Unique identifier of this custom registry object.
     *
     * ```property
     * Identifier: Identifier
     * ```
     */
    private fun luaGetIdentifier(lua: Lua): Int {
        val registryObject = lua.checkUserdata<CustomRegistryObject>(1)
        lua.push(registryObject.identifier, Lua.Conversion.FULL)
        return 1
    }

    /**
     * Unique name of this custom registry object.
     *
     * ```property
     * Name: string
     * ```
     */
    private fun luaGetName(lua: Lua): Int {
        val registryObject = lua.checkUserdata<CustomRegistryObject>(1)
        lua.push(registryObject.identifier.toString())
        return 1
    }

    /**
     * Gets metadata value for the specified key from this custom registry object.
     *
     * ```signatures
     * GetMetadata(key: string) -> any|nil
     * ```
     */
    private fun luaGetMetadata(lua: Lua): Int {
        val registryObject = lua.checkUserdata<CustomRegistryObject>(1)
        val key = lua.checkString(2)
        val value = registryObject.getMetadata(key)
        lua.push(value, Lua.Conversion.FULL)
        return 1
    }

    /**
     * Gets a field value from this custom registry object's JSON element.
     *
     * ```signatures
     * GetField(key: string) -> any|nil
     * ```
     */
    private fun luaGetField(lua: Lua): Int {
        val registryObject = lua.checkUserdata<CustomRegistryObject>(1)
        val key = lua.checkString(2)
        when (val value = (registryObject.element as? JsonObject)?.get(key)) {
            is JsonPrimitive -> lua.push(value.unwrap(), Lua.Conversion.FULL)
            is JsonArray -> lua.push(value.unwrap(), Lua.Conversion.FULL)
            is JsonObject -> lua.push(value.unwrap(), Lua.Conversion.FULL)
            else -> lua.pushNil()
        }
        return 1
    }

    val luaMeta = LuaMappedMetatable(CustomRegistryObject::class) {
        getter(::luaGetIdentifier)
        getter(::luaGetName)
        callable(::luaGetMetadata)
        callable(::luaGetField)
    }
}

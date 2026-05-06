package com.seleneworlds.common.i18n

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.toLocale
import com.seleneworlds.common.lua.util.toSerializedMap

/**
 * Localize messages.
 */
class I18nLuaApi(private val api: I18nApi) : LuaModule {
    override val name = "selene.i18n"

    override fun register(table: LuaValue) {
        table.register("get", this::get)
        table.register("format", this::format)
        table.register("hasKey", this::hasKey)
    }

    private fun get(lua: Lua): Int {
        val value = api.get(lua.checkString(1), lua.toLocale(2))
        if (value != null) {
            lua.push(value)
        } else {
            lua.pushNil()
        }
        return 1
    }

    private fun format(lua: Lua): Int {
        val value = api.format(lua.checkString(1), lua.toSerializedMap(2) ?: emptyMap(), lua.toLocale(3))
        if (value != null) {
            lua.push(value)
        } else {
            lua.pushNil()
        }
        return 1
    }

    private fun hasKey(lua: Lua): Int {
        lua.push(api.hasKey(lua.checkString(1), lua.toLocale(2)))
        return 1
    }
}

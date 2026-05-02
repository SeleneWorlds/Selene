package com.seleneworlds.server.attributes.filters

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.util.CallerInfo
import com.seleneworlds.common.script.ScriptTrace
import com.seleneworlds.common.lua.util.toAny
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.server.attributes.Attribute

class LuaAttributeFilter<T : Any?>(val callback: LuaValue, val registrationSite: CallerInfo) : AttributeFilter<T>,
    ScriptTrace {
    private val logger: Logger = LoggerFactory.getLogger(LuaAttributeFilter::class.java)

    override var enabled: Boolean = true

    override fun apply(attribute: Attribute<T>, value: T): T {
        val lua = callback.state()
        lua.push(callback)
        lua.push(attribute.api, Lua.Conversion.NONE)
        lua.push(value, Lua.Conversion.FULL)
        try {
            lua.xpCall(2, 1, this)
            @Suppress("UNCHECKED_CAST")
            return lua.toAny(-1) as T
        } catch (e: LuaException) {
            logger.error("Lua Error in Attribute Filter", e)
            return value
        }
    }

    override fun scriptTrace(): String {
        return "[attribute filter] registered in $registrationSite"
    }
}

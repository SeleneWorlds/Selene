package world.selene.server.attribute

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.CallerInfo
import world.selene.common.lua.LuaTrace
import world.selene.common.lua.toAny
import world.selene.common.lua.xpCall

class LuaAttributeFilter<T : Any?>(val callback: LuaValue, val registrationSite: CallerInfo) : AttributeFilter<T>,
    LuaTrace {
    private val logger: Logger = LoggerFactory.getLogger(LuaAttributeFilter::class.java)

    override var enabled: Boolean = true

    override fun apply(attribute: Attribute<T>, value: T): T {
        val lua = callback.state()
        lua.push(callback)
        lua.push(attribute, Lua.Conversion.NONE)
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

    override fun luaTrace(): String {
        return "[attribute filter] registered in $registrationSite"
    }
}
package world.selene.server.attribute

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.toAny

class LuaAttributeFilter<T : Any?>(val callback: LuaValue, val filterData: Any?) : AttributeFilter<T> {
    private val logger: Logger = LoggerFactory.getLogger(LuaAttributeFilter::class.java)

    override var enabled: Boolean = true

    override fun apply(attribute: Attribute<T>, value: T): T {
        val lua = callback.state()
        lua.push(callback)
        lua.push(attribute, Lua.Conversion.NONE)
        lua.push(value, Lua.Conversion.FULL)
        lua.push(filterData, Lua.Conversion.FULL)
        try {
            lua.pCall(3, 1)
            @Suppress("UNCHECKED_CAST")
            return lua.toAny(-1) as T
        } catch (e: LuaException) {
            logger.error("Error calling Lua filter", e)
            return value
        }
    }
}
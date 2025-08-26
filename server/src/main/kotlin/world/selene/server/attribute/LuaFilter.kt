package world.selene.server.attribute

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.toAny

class LuaFilter(val callback: LuaValue, val filterData: Any?) : Filter<Any> {
    private val logger: Logger = LoggerFactory.getLogger(LuaFilter::class.java)

    override fun apply(attribute: Attribute<*>, value: Any?): Any? {
        val lua = callback.state()
        lua.push(callback)
        lua.push(attribute, Lua.Conversion.NONE)
        lua.push(value, Lua.Conversion.FULL)
        lua.push(filterData, Lua.Conversion.FULL)
        try {
            lua.pCall(2, 1)
            return lua.toAny(-1)
        } catch (e: LuaException) {
            logger.error("Error calling Lua filter", e)
            return value
        }
    }
}
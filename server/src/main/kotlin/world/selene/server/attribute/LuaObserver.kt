package world.selene.server.attribute

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.ManagedLuaTable

class LuaObserver(val callback: LuaValue, val data: ManagedLuaTable) : Observer {
    private val logger: Logger = LoggerFactory.getLogger(LuaObserver::class.java)

    override fun attributeChanged(attribute: Attribute<*>) {
        val lua = callback.state()
        lua.push(callback)
        lua.push(attribute, Lua.Conversion.NONE)
        lua.push(data, Lua.Conversion.NONE)
        try {
            lua.pCall(2, 0)
        } catch (e: LuaException) {
            logger.error("Error calling Lua observer", e)
        }
    }
}
package world.selene.server.attribute

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

class LuaObserver(val callback: LuaValue, val observerData: Any?) : Observer {
    private val logger: Logger = LoggerFactory.getLogger(LuaObserver::class.java)

    override fun attributeChanged(attribute: Attribute<*>, observableData: Any?) {
        val lua = callback.state()
        lua.push(callback)
        lua.push(attribute, Lua.Conversion.NONE)
        lua.push(observerData, Lua.Conversion.NONE)
        lua.push(observableData, Lua.Conversion.NONE)
        try {
            lua.pCall(3, 0)
        } catch (e: LuaException) {
            logger.error("Error calling Lua observer", e)
        }
    }
}
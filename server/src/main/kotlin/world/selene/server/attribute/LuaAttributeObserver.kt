package world.selene.server.attribute

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

class LuaAttributeObserver(val callback: LuaValue, val observerData: Any?) : AttributeObserver {
    private val logger: Logger = LoggerFactory.getLogger(LuaAttributeObserver::class.java)

    override fun attributeChanged(attribute: Attribute<*>, observableData: Any?) {
        val lua = callback.state()
        lua.push(callback)
        lua.push(attribute, Lua.Conversion.NONE)
        lua.push(observerData, Lua.Conversion.FULL)
        lua.push(observableData, Lua.Conversion.FULL)
        try {
            lua.pCall(3, 0)
        } catch (e: LuaException) {
            logger.error("Error calling Lua observer", e)
        }
    }
}
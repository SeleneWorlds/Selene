package world.selene.server.attribute

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

class LuaAttributeObserver(val callback: LuaValue) : AttributeObserver {
    private val logger: Logger = LoggerFactory.getLogger(LuaAttributeObserver::class.java)

    override fun attributeChanged(attribute: Attribute<*>) {
        val lua = callback.state()
        lua.push(callback)
        lua.push(attribute, Lua.Conversion.NONE)
        try {
            lua.pCall(1, 0)
        } catch (e: LuaException) {
            logger.error("Error calling attribute observer", e)
        }
    }
}
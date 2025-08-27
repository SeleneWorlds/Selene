package world.selene.server.attribute

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

class LuaAttributeViewObserver(val callback: LuaValue) : AttributeViewObserver {
    private val logger: Logger = LoggerFactory.getLogger(LuaAttributeViewObserver::class.java)

    override fun attributeChanged(
        view: AttributeView,
        attributeKey: Any,
        attribute: Attribute<*>
    ) {
        val lua = callback.state()
        lua.push(callback)
        lua.push(view, Lua.Conversion.NONE)
        lua.push(attributeKey, Lua.Conversion.FULL)
        lua.push(attribute, Lua.Conversion.NONE)
        try {
            lua.pCall(3, 0)
        } catch (e: LuaException) {
            logger.error("Error calling attribute view observer", e)
        }
    }
}
package world.selene.common.observable

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

class LuaObserver<T>(val callback: LuaValue) : Observer<T> {
    private val logger: Logger = LoggerFactory.getLogger(LuaObserver::class.java)

    override fun notifyObserver(data: T) {
        val lua = callback.state()
        lua.push(callback)
        lua.push(data, Lua.Conversion.FULL)
        try {
            lua.pCall(1, 0)
        } catch (e: LuaException) {
            logger.error("Error calling observer", e)
        }
    }
}
package world.selene.common.observable

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.CallerInfo
import world.selene.common.lua.LuaTrace
import world.selene.common.lua.xpCall

class LuaObserver<T>(val callback: LuaValue, val registrationSite: CallerInfo) : Observer<T>, LuaTrace {
    private val logger: Logger = LoggerFactory.getLogger(LuaObserver::class.java)

    override fun notifyObserver(data: T) {
        val lua = callback.state()
        lua.push(callback)
        lua.push(data, Lua.Conversion.FULL)
        try {
            lua.xpCall(1, 0, this)
        } catch (e: LuaException) {
            logger.error("Lua Error in Observer", e)
        }
    }

    override fun luaTrace(): String {
        return "[observer] registered in $registrationSite"
    }
}
package world.selene.common.lua

import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

@Suppress("FunctionName", "unused")
class LuaSignal(private val luaManager: LuaManager) {

    data class LuaSignalCallback(val callback: LuaValue, val registrationSite: CallerInfo)

    private val logger = LoggerFactory.getLogger(LuaSignal::class.java)
    private val callbacks = mutableListOf<LuaSignalCallback>()

    fun emit(args: (Lua) -> Int) {
        callbacks.forEach { (callback, registrationSite) ->
            val lua = callback.state()
            try {
                lua.push(callback)
                lua.pCall(args(lua), 0)
            } catch (e: LuaException) {
                logger.error("Error firing lua signal (connected at $registrationSite)", luaManager.sanitizeException(e))
            }
        }
    }

    fun Connect(callback: LuaValue) {
        callbacks.add(LuaSignalCallback(callback, callback.state().getCallerInfo()))
    }

    fun hasListeners(): Boolean {
        return callbacks.isNotEmpty()
    }

}
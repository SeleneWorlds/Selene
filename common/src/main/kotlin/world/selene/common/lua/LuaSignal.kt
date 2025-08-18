package world.selene.common.lua

import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

@Suppress("FunctionName", "unused")
class LuaSignal(private val luaManager: LuaManager) {

    private val logger = LoggerFactory.getLogger(LuaSignal::class.java)
    private val callbacks = mutableListOf<LuaValue>()

    fun emit(args: (Lua) -> Int) {
        callbacks.forEach {
            val lua = it.state()
            try {
                lua.push(it)
                lua.pCall(args(lua), 0)
            } catch (e: LuaException) {
                logger.error("Error firing lua signal", luaManager.sanitizeException(e))
            }
        }
    }

    fun Connect(callback: LuaValue) {
        callbacks.add(callback)
    }

    fun hasListeners(): Boolean {
        return callbacks.isNotEmpty()
    }

}
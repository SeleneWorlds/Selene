package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

@Suppress("FunctionName", "unused")
class LuaSignal {

    private val callbacks = mutableListOf<LuaValue>()

    fun emit(args: Array<LuaValue>) {
        callbacks.forEach { it.call(*args) }
    }

    fun emit(args: (Lua) -> Int) {
        callbacks.forEach {
            val lua = it.state()
            try {
                lua.push(it)
                lua.pCall(args(lua), 0)
            } catch (e: LuaException) {
                throw LuaManager.sanitizeException(e)
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
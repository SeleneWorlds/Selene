package world.selene.common.lua

import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

class Signal(private val name: String) : LuaMetatableProvider {
    data class SignalHandler(val name: String, val callback: LuaValue, val registrationSite: CallerInfo) : LuaTrace {
        override fun luaTrace(): String {
            return "[signal $name] registered in <$registrationSite>"
        }
    }

    private val logger = LoggerFactory.getLogger(Signal::class.java)
    private val callbacks = mutableListOf<SignalHandler>()

    fun emit(args: (Lua) -> Int = { 0 }) {
        callbacks.forEach { handler ->
            val lua = handler.callback.state()
            try {
                lua.push(handler.callback)
                lua.xpCall(args(lua), 0, handler)
            } catch (e: LuaException) {
                logger.error("Lua Error in Signal", e)
            }
        }
    }

    fun hasListeners(): Boolean {
        return callbacks.isNotEmpty()
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {

        private fun luaConnect(lua: Lua): Int {
            val signal = lua.checkUserdata<Signal>(1)
            val callback = lua.checkFunction(2)
            signal.callbacks.add(SignalHandler(signal.name, callback, lua.getCallerInfo()))
            return 0
        }

        val luaMeta = LuaMappedMetatable(Signal::class) {
            callable(::luaConnect)
        }
    }
}
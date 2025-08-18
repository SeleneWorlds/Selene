package world.selene.common.lua

import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

class Signal(private val name: String) : LuaMetatableProvider {
    data class NamedCallback(val callback: LuaValue, val registrationSite: CallerInfo)

    private val logger = LoggerFactory.getLogger(Signal::class.java)
    private val callbacks = mutableListOf<NamedCallback>()

    fun emit(args: (Lua) -> Int = { 0 }) {
        callbacks.forEach { (callback, registrationSite) ->
            val lua = callback.state()
            try {
                lua.push(callback)
                lua.pCall(args(lua), 0)
            } catch (e: LuaException) {
                logger.error("Error firing $name (connected at $registrationSite)", e)
            }
        }
    }

    fun connect(lua: Lua): Int {
        val callback = lua.toLuaValue(2)
        callbacks.add(NamedCallback(callback, callback.state().getCallerInfo()))
        return 0
    }

    fun hasListeners(): Boolean {
        return callbacks.isNotEmpty()
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    companion object {
        val luaMeta = LuaMappedMetatable(Signal::class) {
            callable(Signal::connect)
        }
    }
}
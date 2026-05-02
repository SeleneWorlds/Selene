package com.seleneworlds.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.event.Event
import com.seleneworlds.common.lua.util.checkFunction
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.script.ScriptTrace

class LuaEventSink<T : Any>(private val event: Event<T>, private val factory: (LuaValue, ScriptTrace) -> T) {
    companion object {
        /**
         * Connects a callback function to this signal.
         * The callback will be invoked whenever the signal is emitted.
         *
         * ```signatures
         * Connect(callback: function)
         * ```
         */
        private fun luaConnect(lua: Lua): Int {
            val luaEventSink = lua.checkUserdata<LuaEventSink<Any>>(1)
            val callback = lua.checkFunction(2)
            val listener = luaEventSink.factory(callback, lua.getCallerInfo())
            luaEventSink.event.register(listener)
            return 0
        }

        val luaMeta = LuaMappedMetatable(LuaEventSink::class) {
            callable(::luaConnect)
        }
    }
}

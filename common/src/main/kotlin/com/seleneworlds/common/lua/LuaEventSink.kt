package com.seleneworlds.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleEventSubscriptions
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.event.Event
import com.seleneworlds.common.lua.util.checkFunction
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.script.ScriptTrace
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

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
        private fun connect(lua: Lua): Int {
            val luaEventSink = lua.checkUserdata<LuaEventSink<Any>>(1)
            val callback = lua.checkFunction(2)
            val bundle = BundleExecutionContext.currentBundle
            val listener = luaEventSink.factory(callback, lua.getCallerInfo()).withBundleContext(bundle)
            luaEventSink.event.register(listener)
            BundleEventSubscriptions.record(luaEventSink.event, listener)
            return 0
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Any> T.withBundleContext(bundle: Bundle?): T {
            if (bundle == null) {
                return this
            }

            val interfaces = javaClass.interfaces
            if (interfaces.isEmpty()) {
                return this
            }

            return Proxy.newProxyInstance(javaClass.classLoader, interfaces) { _, method, args ->
                try {
                    BundleExecutionContext.withBundle(bundle) {
                        method.invoke(this, *(args ?: emptyArray()))
                    }
                } catch (e: InvocationTargetException) {
                    throw e.cause ?: e
                }
            } as T
        }

        val luaMeta = LuaMappedMetatable(LuaEventSink::class) {
            callable(::connect)
        }
    }
}

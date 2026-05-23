package com.seleneworlds.common.lua

import party.iroiro.luajava.Lua
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleEventSubscriptions
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.event.Event
import com.seleneworlds.common.event.EventFactory
import com.seleneworlds.common.lua.util.checkFunction
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.toAny
import com.seleneworlds.common.lua.util.xpCall
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

class LuaEvent {
    fun interface Listener {
        fun invoke(arguments: Array<Any?>)
    }

    private val event: Event<Listener> = EventFactory.arrayBackedEvent<Listener> { listeners ->
        Listener { arguments ->
            listeners.forEach { listener ->
                EventFactory.catchLog {
                    listener.invoke(arguments)
                }
            }
        }
    }

    companion object {
        private fun connect(lua: Lua): Int {
            val luaEvent = lua.checkUserdata<LuaEvent>(1)
            val callback = lua.checkFunction(2)
            val trace = lua.getCallerInfo()
            val bundle = BundleExecutionContext.currentBundle
            val listener = withBundleContext(Listener { arguments ->
                val callbackLua = callback.state()
                callbackLua.push(callback)
                arguments.forEach { argument ->
                    if (argument == null) {
                        callbackLua.pushNil()
                    } else {
                        callbackLua.push(argument, Lua.Conversion.FULL)
                    }
                }
                callbackLua.xpCall(arguments.size, 0, trace)
            }, bundle)
            luaEvent.event.register(listener)
            BundleEventSubscriptions.record(luaEvent.event, listener)
            return 0
        }

        private fun fire(lua: Lua): Int {
            val luaEvent = lua.checkUserdata<LuaEvent>(1)
            val arguments = Array(lua.top - 1) { index -> lua.toAny(index + 2) }
            luaEvent.event.invoker().invoke(arguments)
            return 0
        }

        @Suppress("UNCHECKED_CAST")
        internal fun <T : Any> withBundleContext(listener: T, bundle: Bundle?): T {
            if (bundle == null) {
                return listener
            }

            val interfaces = listener.javaClass.interfaces
            if (interfaces.isEmpty()) {
                return listener
            }

            return Proxy.newProxyInstance(listener.javaClass.classLoader, interfaces) { _, method, args ->
                try {
                    BundleExecutionContext.withBundle(bundle) {
                        method.invoke(listener, *(args ?: emptyArray()))
                    }
                } catch (e: InvocationTargetException) {
                    throw e.cause ?: e
                }
            } as T
        }

        val luaMeta = LuaMappedMetatable(LuaEvent::class) {
            callable(::connect)
            callable(::fire)
        }
    }
}

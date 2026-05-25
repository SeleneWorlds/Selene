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
import com.seleneworlds.common.lua.util.xpCall
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

class LuaEvent {
    fun interface Listener {
        fun invoke(argumentsRef: Int, argumentCount: Int)
    }

    private val event: Event<Listener> = EventFactory.arrayBackedEvent<Listener> { listeners ->
        Listener { argumentsRef, argumentCount ->
            listeners.forEach { listener ->
                EventFactory.catchLog {
                    listener.invoke(argumentsRef, argumentCount)
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
            val listener = withBundleContext(Listener { argumentsRef, argumentCount ->
                val callbackLua = callback.state()
                callbackLua.push(callback)
                callbackLua.refGet(argumentsRef)
                val argumentsTableIndex = callbackLua.top
                for (argumentIndex in 1..argumentCount) {
                    callbackLua.rawGetI(argumentsTableIndex, argumentIndex)
                }
                callbackLua.remove(argumentsTableIndex)
                callbackLua.xpCall(argumentCount, 0, trace)
            }, bundle)
            luaEvent.event.register(listener)
            BundleEventSubscriptions.record(luaEvent.event, listener)
            return 0
        }

        private fun fire(lua: Lua): Int {
            val luaEvent = lua.checkUserdata<LuaEvent>(1)
            val argumentCount = lua.top - 1
            lua.createTable(argumentCount, 0)
            for (argumentIndex in 1..argumentCount) {
                lua.pushValue(argumentIndex + 1)
                lua.rawSetI(-2, argumentIndex)
            }
            val argumentsRef = lua.ref()
            try {
                luaEvent.event.invoker().invoke(argumentsRef, argumentCount)
            } finally {
                lua.unref(argumentsRef)
            }
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

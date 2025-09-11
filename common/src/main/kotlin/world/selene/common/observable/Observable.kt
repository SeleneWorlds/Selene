package world.selene.common.observable

import party.iroiro.luajava.Lua
import world.selene.common.lua.*
import world.selene.common.lua.util.checkFunction
import world.selene.common.lua.util.checkUserdata
import world.selene.common.lua.util.getCallerInfo
import world.selene.common.lua.util.throwTypeError
import world.selene.common.lua.util.toAny

interface Observable<T> {
    fun subscribe(observer: Observer<T>)
    fun unsubscribe(observer: Observer<T>)
    fun notifyObservers(data: T)

    @Suppress("SameReturnValue")
    companion object {
        /**
         * Subscribes an observer to this observable.
         * Can accept either a function or an Observer object.
         *
         * ```signatures
         * Subscribe(observer: function(data: any)) -> Observer
         * Subscribe(observer: Observer) -> Observer
         * ```
         */
        private fun luaSubscribe(lua: Lua): Int {
            val observable = lua.checkUserdata<Observable<Any>>(1)
            val registrationSite = lua.getCallerInfo()
            val observer = when (lua.type(2)) {
                Lua.LuaType.FUNCTION -> LuaObserver(lua.checkFunction(2), registrationSite)
                Lua.LuaType.USERDATA -> lua.checkUserdata<Observer<Any>>(2)
                else -> lua.throwTypeError(2, Observer::class)
            }
            observable.subscribe(observer)
            lua.push(observer, Lua.Conversion.NONE)
            return 1
        }

        /**
         * Unsubscribes an observer from this observable.
         *
         * ```signatures
         * Unsubscribe(observer: Observer)
         * ```
         */
        private fun luaUnsubscribe(lua: Lua): Int {
            val observable = lua.checkUserdata<Observable<Any>>(1)
            val observer = lua.checkUserdata<Observer<Any>>(2)
            observable.unsubscribe(observer)
            return 0
        }

        /**
         * Notifies all observers with the given data.
         *
         * ```signatures
         * NotifyObservers()
         * NotifyObservers(data: any)
         * ```
         */
        private fun luaNotifyObservers(lua: Lua): Int {
            val observable = lua.checkUserdata<Observable<Any>>(1)
            val data = lua.toAny(2) ?: observable
            observable.notifyObservers(data)
            return 0
        }

        val luaMeta = LuaMappedMetatable(Observable::class) {
            callable(::luaSubscribe)
            callable(::luaUnsubscribe)
            callable(::luaNotifyObservers)
        }
    }
}
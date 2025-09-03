package world.selene.common.observable

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.checkFunction
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.getCallerInfo
import world.selene.common.lua.throwTypeError
import world.selene.common.lua.toAny

interface Observable<T> {
    fun subscribe(observer: Observer<T>)
    fun unsubscribe(observer: Observer<T>)
    fun notifyObservers(data: T)

    companion object {
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

        private fun luaUnsubscribe(lua: Lua): Int {
            val observable = lua.checkUserdata<Observable<Any>>(1)
            val observer = lua.checkUserdata<Observer<Any>>(2)
            observable.unsubscribe(observer)
            return 0
        }

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
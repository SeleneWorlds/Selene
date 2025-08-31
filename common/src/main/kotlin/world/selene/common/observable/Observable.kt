package world.selene.common.observable

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.checkFunction
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.throwTypeError
import world.selene.common.lua.toAny

interface Observable<T> {
    fun subscribe(observer: Observer<T>)
    fun unsubscribe(observer: Observer<T>)
    fun notifyObservers(data: T)

    companion object {
        val luaMeta = LuaMappedMetatable(Observable::class) {
            callable("Subscribe") { lua ->
                @Suppress("UNCHECKED_CAST")
                val observable = lua.checkSelf() as Observable<Any>
                val observer = when (lua.type(2)) {
                    Lua.LuaType.FUNCTION -> LuaObserver(lua.checkFunction(2))
                    Lua.LuaType.USERDATA -> lua.checkUserdata<Observer<Any>>(2)
                    else -> lua.throwTypeError(2, Observer::class)
                }
                observable.subscribe(observer)
                lua.push(observer, Lua.Conversion.NONE)
                1
            }
            callable("Unsubscribe") { lua ->
                @Suppress("UNCHECKED_CAST")
                val observable = lua.checkSelf() as Observable<Any>
                val observer = lua.checkUserdata<Observer<Any>>(2)
                observable.unsubscribe(observer)
                0
            }
            callable("NotifyObservers") { lua ->
                @Suppress("UNCHECKED_CAST")
                val observable = lua.checkSelf() as Observable<Any>
                val data = lua.toAny(2) ?: observable
                observable.notifyObservers(data)
                0
            }
        }
    }
}
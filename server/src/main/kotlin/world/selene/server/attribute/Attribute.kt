package world.selene.server.attribute

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.lua.checkFunction
import world.selene.common.lua.checkString
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.getCallerInfo
import world.selene.common.lua.throwTypeError
import world.selene.common.observable.Observable
import world.selene.common.observable.Observer

class Attribute<T : Any?>(val owner: Any, val name: String, initialValue: T) : LuaMetatableProvider, Observable<Attribute<*>> {
    val observers = mutableListOf<Observer<Attribute<*>>>()
    val constraints = mutableListOf<AttributeFilter<T>>()
    val constraintsByName = mutableMapOf<String, AttributeFilter<T>>()
    val modifiers = mutableListOf<AttributeFilter<T>>()
    val modifiersByName = mutableMapOf<String, AttributeFilter<T>>()

    var value: T = initialValue
        set(value) {
            val prev = field
            if (prev != value) {
                field = value
                constraints.forEach {
                    if (it.enabled) {
                        field = it.apply(this, field)
                    }
                }
                notifyObservers(this)
            }
        }

    val effectiveValue: T
        get() {
            var value = this.value
            modifiers.forEach {
                if (it.enabled) {
                    value = it.apply(this, value)
                }
            }
            return value
        }

    override fun subscribe(observer: Observer<Attribute<*>>) {
        observers.add(observer)
    }

    override fun unsubscribe(observer: Observer<Attribute<*>>) {
        observers.remove(observer)
    }

    override fun notifyObservers(data: Attribute<*>) {
        observers.forEach { it.notifyObserver(this) }
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return luaMeta
    }

    override fun toString(): String {
        return "Attribute($owner.$name = $value)"
    }

    companion object {
        private fun luaAddConstraint(lua: Lua): Int {
            val attribute = lua.checkUserdata<Attribute<Any?>>(1)
            val name = lua.checkString(2)
            val registrationSite = lua.getCallerInfo()
            val filter = when (lua.type(3)) {
                Lua.LuaType.FUNCTION -> LuaAttributeFilter(lua.checkFunction(3), registrationSite)
                Lua.LuaType.USERDATA -> lua.checkUserdata<AttributeFilter<Any?>>(3)
                else -> lua.throwTypeError(3, AttributeFilter::class)
            }
            attribute.addConstraint(name, filter)
            lua.push(filter, Lua.Conversion.NONE)
            return 1
        }

        private fun luaRemoveConstraint(lua: Lua): Int {
            val attribute = lua.checkUserdata<Attribute<Any?>>(1)
            attribute.removeConstraint(lua.checkString(2))
            return 0
        }

        private fun luaAddModifier(lua: Lua): Int {
            val attribute = lua.checkUserdata<Attribute<Any?>>(1)
            val registrationSite = lua.getCallerInfo()
            val name = lua.checkString(2)
            val filter = when (lua.type(3)) {
                Lua.LuaType.FUNCTION -> LuaAttributeFilter(lua.checkFunction(3), registrationSite)
                Lua.LuaType.USERDATA -> lua.checkUserdata<AttributeFilter<Any?>>(3)
                else -> lua.throwTypeError(3, AttributeFilter::class)
            }
            attribute.addModifier(name, filter)
            lua.push(filter, Lua.Conversion.NONE)
            return 1
        }

        private fun luaRemoveModifier(lua: Lua): Int {
            val attribute = lua.checkUserdata<Attribute<Any?>>(1)
            attribute.removeModifier(lua.checkString(2))
            return 0
        }

        private fun luaRefresh(lua: Lua): Int {
            val attribute = lua.checkUserdata<Attribute<Any?>>(1)
            attribute.notifyObservers(attribute)
            return 0
        }

        val luaMeta = Observable.luaMeta.extend(Attribute::class) {
            readOnly(Attribute<*>::name)
            writable(Attribute<*>::value)
            readOnly(Attribute<*>::effectiveValue)
            readOnly(Attribute<*>::owner)
            callable(::luaAddConstraint)
            callable(::luaRemoveConstraint)
            callable(::luaAddModifier)
            callable(::luaRemoveModifier)
            callable(::luaRefresh)
        }
    }

    private fun addModifier(name: String, filter: AttributeFilter<T>) {
        modifiersByName.put(name, filter)?.let {
            modifiers.remove(it)
        }
        modifiers.add(filter)
    }

    private fun removeModifier(name: String) {
        modifiersByName.remove(name)?.let {
            modifiers.remove(it)
        }
    }

    private fun addConstraint(name: String, filter: AttributeFilter<T>) {
        constraintsByName.put(name, filter)?.let {
            constraints.remove(it)
        }
        constraints.add(filter)
    }

    private fun removeConstraint(name: String) {
        constraintsByName.remove(name)?.let {
            constraints.remove(it)
        }
    }
}
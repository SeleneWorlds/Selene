package world.selene.server.attributes

import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMetatable
import world.selene.common.lua.LuaMetatableProvider
import world.selene.common.observable.Observable
import world.selene.common.observable.Observer
import world.selene.server.entities.Entity
import world.selene.server.players.Player

class AttributeApi(val attribute: Attribute<Any?>) : LuaMetatableProvider, Observable<Attribute<*>> {

    fun getName(): String {
        return attribute.name
    }

    fun getValue(): Any? {
        return attribute.value
    }

    fun setValue(value: Any?) {
        attribute.value = value
    }

    fun getEffectiveValue(): Any? {
        return attribute.effectiveValue
    }

    fun getOwner(): Any {
        return when (val owner = attribute.owner) {
            is Entity -> owner.api
            is Player -> owner.api
            else -> owner
        }
    }

    fun refresh() {
        attribute.notifyObservers(attribute)
    }

    override fun subscribe(observer: Observer<Attribute<*>>) {
        attribute.subscribe(observer)
    }

    override fun unsubscribe(observer: Observer<Attribute<*>>) {
        attribute.unsubscribe(observer)
    }

    override fun notifyObservers(data: Attribute<*>) {
        attribute.notifyObservers(data)
    }

    override fun luaMetatable(lua: Lua): LuaMetatable {
        return AttributeLuaApi.luaMeta
    }
}

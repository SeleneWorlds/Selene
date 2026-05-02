package world.selene.server.attributes

import world.selene.common.observable.Observable
import world.selene.common.observable.Observer
import world.selene.server.entities.Entity
import world.selene.server.players.Player

class AttributeApi(val attribute: Attribute<Any?>) : Observable<AttributeApi> {

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
        attribute.notifyObservers(attribute.api)
    }

    override fun subscribe(observer: Observer<AttributeApi>) {
        attribute.subscribe(observer)
    }

    override fun unsubscribe(observer: Observer<AttributeApi>) {
        attribute.unsubscribe(observer)
    }

    override fun notifyObservers(data: AttributeApi) {
        attribute.notifyObservers(data)
    }
}

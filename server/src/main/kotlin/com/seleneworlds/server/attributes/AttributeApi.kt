package com.seleneworlds.server.attributes

import com.seleneworlds.common.observable.Observable
import com.seleneworlds.common.observable.Observer
import com.seleneworlds.server.entities.Entity
import com.seleneworlds.server.players.Player

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

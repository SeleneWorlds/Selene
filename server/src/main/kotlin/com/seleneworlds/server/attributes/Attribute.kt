package com.seleneworlds.server.attributes

import com.seleneworlds.common.observable.Observable
import com.seleneworlds.common.observable.Observer
import com.seleneworlds.common.script.ExposedApi
import com.seleneworlds.server.attributes.filters.AttributeFilter

class Attribute<T : Any?>(val owner: Any, val name: String, initialValue: T) : Observable<AttributeApi>,
    ExposedApi<AttributeApi> {
    @Suppress("UNCHECKED_CAST")
    override val api = AttributeApi(this as Attribute<Any?>)
    val observers = mutableListOf<Observer<AttributeApi>>()
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
                notifyObservers(api)
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

    override fun subscribe(observer: Observer<AttributeApi>) {
        observers.add(observer)
    }

    override fun unsubscribe(observer: Observer<AttributeApi>) {
        observers.remove(observer)
    }

    override fun notifyObservers(data: AttributeApi) {
        observers.forEach { it.notifyObserver(api) }
    }

    override fun toString(): String {
        return "Attribute($owner.$name = $value)"
    }

    fun addModifier(name: String, filter: AttributeFilter<T>) {
        modifiersByName.put(name, filter)?.let {
            modifiers.remove(it)
        }
        modifiers.add(filter)
    }

    fun removeModifier(name: String) {
        modifiersByName.remove(name)?.let {
            modifiers.remove(it)
        }
    }

    fun addConstraint(name: String, filter: AttributeFilter<T>) {
        constraintsByName.put(name, filter)?.let {
            constraints.remove(it)
        }
        constraints.add(filter)
    }

    fun removeConstraint(name: String) {
        constraintsByName.remove(name)?.let {
            constraints.remove(it)
        }
    }
}

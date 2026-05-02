package com.seleneworlds.common.observable

class ObservableMap(val map: MutableMap<String, Any?> = mutableMapOf()) : Observable<ObservableMap> {

    private var observers: MutableList<Observer<ObservableMap>>? = null

    override fun subscribe(observer: Observer<ObservableMap>) {
        observers?.add(observer) ?: mutableListOf(observer).let { observers = it }
    }

    override fun unsubscribe(observer: Observer<ObservableMap>) {
        observers?.remove(observer)
    }

    override fun notifyObservers(data: ObservableMap) {
        observers?.forEach { it.notifyObserver(data) }
    }

    operator fun get(key: String): Any? {
        return map[key]
    }

    operator fun set(key: String, value: Any?) {
        map[key] = value
    }

    fun remove(key: String) {
        map.remove(key)
    }

    override fun toString(): String {
        return map.toString()
    }

    fun deepCopy(): ObservableMap {
        return ObservableMap(deepCopyMap(map))
    }

    private fun deepCopyMap(original: MutableMap<String, Any?>): MutableMap<String, Any?> {
        val copy = mutableMapOf<String, Any?>()
        for ((key, value) in original) {
            copy[key] = deepCopyValue(value)
        }
        return copy
    }

    private fun deepCopyValue(value: Any?): Any? {
        return when (value) {
            is ObservableMap -> value.deepCopy()
            is MutableMap<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                deepCopyMap(value as MutableMap<String, Any?>)
            }

            is MutableList<*> -> {
                value.map { deepCopyValue(it) }.toMutableList()
            }

            is List<*> -> value.map { deepCopyValue(it) }

            is MutableSet<*> -> {
                value.map { deepCopyValue(it) }.toMutableSet()
            }

            is Set<*> -> value.map { deepCopyValue(it) }.toSet()

            else -> value
        }
    }

}

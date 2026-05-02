package world.selene.common.observable

class ObservableMap(val map: MutableMap<Any, Any> = mutableMapOf()) : Observable<ObservableMap> {

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

    operator fun get(key: Any): Any? {
        return map[key]
    }

    operator fun set(key: Any, value: Any) {
        map[key] = value
    }

    fun remove(key: Any) {
        map.remove(key)
    }

    override fun toString(): String {
        return map.toString()
    }

    fun deepCopy(): ObservableMap {
        return ObservableMap(deepCopyMap(map))
    }

    private fun deepCopyMap(original: MutableMap<Any, Any>): MutableMap<Any, Any> {
        val copy = mutableMapOf<Any, Any>()
        for ((key, value) in original) {
            copy[key] = deepCopyValue(value)
        }
        return copy
    }

    private fun deepCopyValue(value: Any): Any {
        return when (value) {
            is ObservableMap -> value.deepCopy()
            is MutableMap<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                deepCopyMap(value as MutableMap<Any, Any>)
            }

            is MutableList<*> -> {
                @Suppress("UNCHECKED_CAST")
                (value as MutableList<Any>).map { deepCopyValue(it) }.toMutableList()
            }

            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                (value as List<Any>).map { deepCopyValue(it) }
            }

            is MutableSet<*> -> {
                @Suppress("UNCHECKED_CAST")
                (value as MutableSet<Any>).map { deepCopyValue(it) }.toMutableSet()
            }

            is Set<*> -> {
                @Suppress("UNCHECKED_CAST")
                (value as Set<Any>).map { deepCopyValue(it) }.toSet()
            }

            else -> value
        }
    }

}
package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.observable.Observable
import world.selene.common.observable.Observer

class ObservableMap(val map: MutableMap<Any, Any> = mutableMapOf()) : LuaMetatable, Observable<ObservableMap> {

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

    override fun luaGet(lua: Lua): Int {
        val key = when {
            lua.isInteger(2) -> lua.toInteger(2).toInt()
            lua.isString(2) -> lua.toString(2)!!
            else -> return lua.pushError("Key must be a string or number, got ${lua.type(2)}")
        }
        if (key is String && luaMeta.has(key)) {
            return luaMeta.luaGet(lua)
        }
        val value = map[key]
        if (value != null) {
            if (value !is LuaValue && value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                lua.push(ObservableMap(value as MutableMap<Any, Any>), Lua.Conversion.NONE)
            } else if (value !is LuaValue && value is Collection<*>) {
                // TODO would be nice if we had an ObservableList counterpart for this
                lua.throwError("Cannot directly access a table field of an observed map. Use Lookup(\"${key}\") instead to create a local copy.")
            } else if (value is LuaReference<*, *>) {
                lua.push(value.resolve(), Lua.Conversion.NONE)
            } else {
                lua.push(value, Lua.Conversion.FULL)
            }
            return 1
        }
        return lua.pushNil().let { 1 }
    }

    override fun luaSet(lua: Lua): Int {
        val key = when {
            lua.isInteger(2) -> lua.toInteger(2).toInt()
            lua.isString(2) -> lua.toString(2)!!
            else -> return lua.pushNil().let { 1 }
        }
        val value = lua.toAny(3)
        if (value is LuaReferencable<*, *>) {
            map[key] = value.luaReference()
        } else if (value != null) {
            map[key] = value
        } else {
            map.remove(key)
        }
        return 1
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

    companion object {
        private fun getMapValue(container: Any?, key: Any): Any? {
            return when (container) {
                is Map<*, *> -> container[key]
                is ObservableMap -> container.map[key]
                else -> null
            }
        }

        private fun luaPairs(lua: Lua): Int {
            val observableMap = lua.checkUserdata<ObservableMap>(1)
            lua.push { lua ->
                val iterator = lua.checkUserdata<Iterator<Map.Entry<*, *>>>(1)
                if (iterator.hasNext()) {
                    val entry = iterator.next()
                    val value = entry.value
                    lua.push(entry.key, Lua.Conversion.FULL)
                    when (value) {
                        is MutableMap<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
                            lua.push(ObservableMap(value as MutableMap<Any, Any>), Lua.Conversion.NONE)
                        }

                        is LuaReference<*, *> -> {
                            lua.push(value.resolve(), Lua.Conversion.NONE)
                        }

                        else -> {
                            lua.push(value, Lua.Conversion.FULL)
                        }
                    }
                    2
                } else {
                    0
                }
            }
            lua.push(observableMap.map.iterator(), Lua.Conversion.NONE)
            lua.pushNil()
            return 3
        }

        private fun luaToTable(lua: Lua): Int {
            val observableMap = lua.checkUserdata<ObservableMap>(1)
            lua.push(observableMap.map, Lua.Conversion.FULL)
            return 1
        }

        private fun luaRawLookup(lua: Lua): Int {
            val observableMap = lua.checkUserdata<ObservableMap>(1)
            var result: Any? = observableMap.map
            for (index in 2..lua.top) {
                val key = lua.toAny(index) ?: lua.throwTypeError(index, Lua.LuaType.STRING)
                result = getMapValue(result, key)
                if (result == null) {
                    return lua.pushNil().let { 1 }
                }
            }
            lua.push(result, Lua.Conversion.FULL)
            return 1
        }

        private fun luaLookup(lua: Lua): Int {
            val observableMap = lua.checkUserdata<ObservableMap>(1)
            var result: Any? = observableMap.map
            for (index in 2..lua.top) {
                val key = lua.toAny(index) ?: lua.throwTypeError(index, Lua.LuaType.STRING)
                result = getMapValue(result, key) ?: return lua.pushNil().let { 1 }
            }
            if (result !is LuaValue && result is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                lua.push(ObservableMap(result as MutableMap<Any, Any>), Lua.Conversion.NONE)
            } else if (result is LuaReference<*, *>) {
                lua.push(result.resolve(), Lua.Conversion.NONE)
            } else {
                lua.push(result, Lua.Conversion.FULL)
            }
            return 1
        }

        private fun luaSetNested(lua: Lua): Int {
            val observableMap = lua.checkUserdata<ObservableMap>(1)
            var container: Any? = observableMap.map

            for (index in 2..lua.top - 2) {
                val key = lua.toAny(index) ?: lua.throwTypeError(index, Lua.LuaType.STRING)
                val next = getMapValue(container, key)
                    ?: lua.throwError("Cannot set field, key [$key] does not exist")
                if (next !is Map<*, *> && next !is ObservableMap) {
                    lua.throwError("Cannot set field, key [$key] is not a table")
                }
                container = next
            }

            val key = lua.toAny(-2) ?: lua.throwTypeError(-2, Lua.LuaType.STRING)
            val value = lua.toAny(-1)

            when (container) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST") val mutableMap = container as MutableMap<Any, Any>
                    if (value != null) {
                        mutableMap[key] = value
                    } else {
                        mutableMap.remove(key)
                    }
                }

                is ObservableMap -> {
                    if (value != null) {
                        container.map[key] = value
                    } else {
                        container.map.remove(key)
                    }
                }
            }
            return 0
        }

        private fun luaDeepCopy(lua: Lua): Int {
            val observableMap = lua.checkUserdata<ObservableMap>(1)
            lua.push(observableMap.deepCopy(), Lua.Conversion.NONE)
            return 1
        }

        private fun luaHasKey(lua: Lua): Int {
            val observableMap = lua.checkUserdata<ObservableMap>(1)
            val key = lua.toAny(2) ?: lua.throwTypeError(2, Lua.LuaType.STRING)
            lua.push(observableMap.map.containsKey(key))
            return 1
        }

        val luaMeta = Observable.luaMeta.extend(ObservableMap::class) {
            callable(:: luaPairs)
            callable(::luaToTable)
            callable(::luaRawLookup)
            callable(::luaLookup)
            callable(::luaSetNested, "Set")
            callable(::luaDeepCopy)
            callable(::luaHasKey)
        }
    }
}
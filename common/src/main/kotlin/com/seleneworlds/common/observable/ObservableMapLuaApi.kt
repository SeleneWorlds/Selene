package com.seleneworlds.common.observable

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.util.IdResolvable
import com.seleneworlds.common.lua.LuaMetatable
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.pushError
import com.seleneworlds.common.lua.util.throwError
import com.seleneworlds.common.lua.util.throwTypeError
import com.seleneworlds.common.lua.util.toAny
import com.seleneworlds.common.util.ResolvableReference
import kotlin.collections.set

object ObservableMapLuaApi {
    private fun getMapValue(container: Any?, key: String): Any? {
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
                        lua.push(ObservableMap(value as MutableMap<String, Any?>), Lua.Conversion.NONE)
                    }

                    is ResolvableReference<*, *> -> {
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
            val key = lua.toString(index) ?: lua.throwTypeError(index, Lua.LuaType.STRING)
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
            val key = lua.toString(index) ?: lua.throwTypeError(index, Lua.LuaType.STRING)
            result = getMapValue(result, key) ?: return lua.pushNil().let { 1 }
        }
        if (result !is LuaValue && result is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            lua.push(ObservableMap(result as MutableMap<String, Any?>), Lua.Conversion.NONE)
        } else if (result is ResolvableReference<*, *>) {
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
            val key = lua.toString(index) ?: lua.throwTypeError(index, Lua.LuaType.STRING)
            val next = getMapValue(container, key)
                ?: lua.throwError("Cannot set field, key [$key] does not exist")
            if (next !is Map<*, *> && next !is ObservableMap) {
                lua.throwError("Cannot set field, key [$key] is not a table")
            }
            container = next
        }

        val key = lua.toString(-2) ?: lua.throwTypeError(-2, Lua.LuaType.STRING)
        val value = lua.toAny(-1)

        when (container) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST") val mutableMap = container as MutableMap<String, Any?>
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
        val key = lua.toString(2) ?: lua.throwTypeError(2, Lua.LuaType.STRING)
        lua.push(observableMap.map.containsKey(key))
        return 1
    }

    val luaMappings = ObservableLuaApi.luaMeta.extend(ObservableMap::class) {
        callable(::luaPairs)
        callable(::luaToTable)
        callable(::luaRawLookup)
        callable(::luaLookup)
        callable(::luaSetNested, "Set")
        callable(::luaDeepCopy)
        callable(::luaHasKey)
    }

    val luaMeta = object : LuaMetatable {
        override fun luaGet(lua: Lua): Int {
            val map = lua.checkUserdata<ObservableMap>(1)
            val key = lua.toString(2) ?: return lua.pushError("Key must be a string, got ${lua.type(2)}")
            if (luaMappings.has(key)) {
                return luaMappings.luaGet(lua)
            }
            val value = map[key]
            if (value != null) {
                if (value !is LuaValue && value is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    lua.push(ObservableMap(value as MutableMap<String, Any?>), Lua.Conversion.NONE)
                } else if (value !is LuaValue && value is Collection<*>) {
                    // TODO would be nice if we had an ObservableList counterpart for this
                    lua.throwError("Cannot directly access a table field of an observed map. Use Lookup(\"${key}\") instead to create a local copy.")
                } else if (value is ResolvableReference<*, *>) {
                    lua.push(value.resolve(), Lua.Conversion.NONE)
                } else {
                    lua.push(value, Lua.Conversion.FULL)
                }
                return 1
            }
            return lua.pushNil().let { 1 }
        }

        override fun luaSet(lua: Lua): Int {
            val map = lua.checkUserdata<ObservableMap>(1)
            val key = lua.toString(2) ?: return lua.pushError("Key must be a string, got ${lua.type(2)}")
            val value = lua.toAny(3)
            if (value is IdResolvable<*, *>) {
                map[key] = value.resolvableReference()
            } else if (value != null) {
                map[key] = value
            } else {
                map.remove(key)
            }
            return 0
        }

        override fun luaToString(lua: Lua): String {
            val map = lua.checkUserdata<ObservableMap>(1)
            return map.toString()
        }
    }
}

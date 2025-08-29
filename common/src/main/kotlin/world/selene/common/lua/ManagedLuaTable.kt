package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class ManagedLuaTable(val map: MutableMap<Any, Any> = mutableMapOf()) : LuaMetatable {

    override fun luaGet(lua: Lua): Int {
        val key = when {
            lua.isInteger(2) -> lua.toInteger(2).toInt()
            lua.isString(2) -> lua.toString(2)!!
            else -> return 0
        }
        if (key is String && luaMeta.has(key)) {
            return luaMeta.luaGet(lua)
        }
        val value = map[key]
        if (value != null) {
            if (value !is LuaValue && value is Map<*, *>) {
                @Suppress("UNCHECKED_CAST")
                lua.push(ManagedLuaTable(value as MutableMap<Any, Any>), Lua.Conversion.NONE)
            } else if (value !is LuaValue && value is Collection<*>) {
                lua.throwError("Cannot directly access a table field of a managed table. Use Lookup(\"${key}\") instead to create a local copy.")
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

    companion object {
        private fun getMapValue(container: Any?, key: Any): Any? {
            return when (container) {
                is Map<*, *> -> container[key]
                is ManagedLuaTable -> container.map[key]
                else -> null
            }
        }

        val luaMeta = LuaMappedMetatable(ManagedLuaTable::class) {
            callable("Pairs") { lua ->
                val managedLuaTable = lua.checkSelf()
                lua.push { innerLua ->
                    val iterator = innerLua.checkUserdata<Iterator<Map.Entry<*, *>>>(1)
                    if (iterator.hasNext()) {
                        val entry = iterator.next()
                        val value = entry.value
                        innerLua.push(entry.key, Lua.Conversion.FULL)
                        when (value) {
                            is MutableMap<*, *> -> {
                                @Suppress("UNCHECKED_CAST")
                                innerLua.push(ManagedLuaTable(value as MutableMap<Any, Any>), Lua.Conversion.NONE)
                            }

                            is LuaReference<*, *> -> {
                                innerLua.push(value.resolve(), Lua.Conversion.NONE)
                            }

                            else -> {
                                innerLua.push(value, Lua.Conversion.FULL)
                            }
                        }
                        2
                    } else {
                        0
                    }
                }
                lua.push(managedLuaTable.map.iterator(), Lua.Conversion.NONE)
                lua.pushNil()
                3
            }
            callable("ToTable") { lua ->
                val managedTable = lua.checkSelf()
                lua.push(managedTable.map, Lua.Conversion.FULL)
                1
            }
            callable("Lookup") { lua ->
                val managedTable = lua.checkSelf()
                var result: Any? = managedTable.map
                for (index in 2..lua.top) {
                    val key = lua.toAny(index) ?: lua.throwTypeError(index, Lua.LuaType.STRING)
                    result = getMapValue(result, key) ?: return@callable lua.pushNil().let { 1 }
                }
                lua.push(result, Lua.Conversion.FULL)
                1
            }
            callable("Set") { lua ->
                val managedTable = lua.checkSelf()
                var container: Any? = managedTable.map

                for (index in 2..lua.top - 2) {
                    val key = lua.toAny(index) ?: lua.throwTypeError(index, Lua.LuaType.STRING)
                    val next = getMapValue(container, key)
                        ?: lua.throwError("Cannot set field, key [$key] does not exist")
                    if (next !is Map<*, *> && next !is ManagedLuaTable) {
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

                    is ManagedLuaTable -> {
                        if (value != null) {
                            container.map[key] = value
                        } else {
                            container.map.remove(key)
                        }
                    }
                }
                0
            }
        }
    }
}
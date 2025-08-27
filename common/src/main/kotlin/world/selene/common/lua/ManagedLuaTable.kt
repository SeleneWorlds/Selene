package world.selene.common.lua

import party.iroiro.luajava.Lua

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
            if (value is Map<*, *> || value is Collection<*>) {
                lua.throwError("Cannot directly access a table field of a managed table. Use Lookup(\"${key}\") instead to create a local copy.")
            }
            lua.push(value, Lua.Conversion.FULL)
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
        if (value != null) {
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
            callable("Lookup") { lua ->
                val managedTable = lua.checkSelf()
                var result: Any? = managedTable.map
                for (index in 2..lua.top) {
                    val key = lua.toAny(index) ?: lua.throwTypeError(index, Lua.LuaType.STRING)
                    result = getMapValue(result, key) ?: return@callable 0
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
                        val mutableMap = container as MutableMap<Any, Any>
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
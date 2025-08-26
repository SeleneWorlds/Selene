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
            lua.push(value, Lua.Conversion.NONE)
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
        val value = lua.toAny(3) ?: return lua.pushNil().let { 1 }
        map[key] = value
        return 1
    }

    override fun toString(): String {
        return map.toString()
    }

    companion object {
        val luaMeta = LuaMappedMetatable(ManagedLuaTable::class) {
            callable("Lookup") { lua ->
                val managedTable = lua.checkSelf()
                var result: Any? = managedTable.map
                for (index in 2..lua.top) {
                    val key = lua.toAny(index)
                    result = when (result) {
                        null -> return@callable 0
                        is Map<*, *> -> result[key]
                        is ManagedLuaTable -> result.map[key]
                        else -> return@callable 0
                    }
                }
                lua.push(result, Lua.Conversion.FULL)
                1
            }
        }
    }
}
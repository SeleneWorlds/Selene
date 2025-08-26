package world.selene.common.lua

import party.iroiro.luajava.Lua

class ManagedLuaTable(val map: MutableMap<Any, Any> = mutableMapOf()) : LuaMetatable {

    override fun luaGet(lua: Lua): Int {
        val key = when {
            lua.isString(2) -> lua.toString(2)!!
            lua.isInteger(2) -> lua.toInteger(2).toInt()
            else -> return 0
        }
        if (key is String && luaMeta.has(key)) {
            return luaMeta.luaGet(lua)
        }
        val value = map[key]
        if (value != null) {
            if (value is Map<*, *> || value is Collection<*>) {
                lua.throwError("Cannot directly access a table field of a managed table. Use :ToTable(\"${key}\") instead to create a copy.")
            }
            lua.push(value, Lua.Conversion.NONE)
            return 1
        }
        return lua.pushNil().let { 1 }
    }

    override fun luaSet(lua: Lua): Int {
        val key = when {
            lua.isString(2) -> lua.toString(2)!!
            lua.isInteger(2) -> lua.toInteger(2).toInt()
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
            callable("ToTable") { lua ->
                val managedTable = lua.checkSelf()
                val key = lua.toString(2)
                if (key == null) {
                    lua.push(managedTable.map)
                } else {
                    when (val value = managedTable.map[key]) {
                        is Map<*, *>, is Collection<*> -> {
                            lua.push(value, Lua.Conversion.FULL)
                        }

                        null -> {
                            lua.pushNil()
                        }

                        else -> {
                            lua.throwError("Expected $key to be a table value, but was ${value::class.simpleName}")
                        }
                    }
                }
                1
            }
        }
    }
}
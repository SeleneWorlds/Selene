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

    companion object {
        val luaMeta = LuaMappedMetatable(ManagedLuaTable::class) {
            callable("ToTable") { lua ->
                val managedTable = lua.checkSelf()
                lua.push(managedTable.map)
                1
            }
        }
    }
}
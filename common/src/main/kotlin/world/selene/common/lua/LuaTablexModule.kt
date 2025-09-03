package world.selene.common.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class LuaTablexModule : LuaModule {
    override val name: String = "tablex"
    override val registerAsGlobal: Boolean = true

    override fun register(table: LuaValue) {
        table.register("managed", this::luaManaged)
        table.register("find", this::luaFind)
        table.register("tostring", this::luaToString)
    }

    private fun luaManaged(lua: Lua): Int {
        val data = lua.toAnyMap(1) as MutableMap?
        lua.push(ManagedLuaTable(data ?: mutableMapOf()), Lua.Conversion.NONE)
        return 1
    }

    private fun luaFind(lua: Lua): Int {
        lua.checkType(1, Lua.LuaType.TABLE)
        lua.top = 2

        var idx = 1
        lua.pushNil() // initial key for next call

        // Stack: table(1), target(2), nil(3)
        while (lua.next(1) != 0) { // pushes key-value pair
            // Stack: table(1), target(2), key(3), value(4)

            // Compare the value with target
            if (lua.equal(4, 2)) {
                lua.pushValue(3) // push the key as return value
                return 1
            }

            lua.pop(1) // pop value, keep key for next iteration
            // Stack: table(1), target(2), key(3)
            idx++
        }

        // Return nil if not found
        lua.pushNil()
        return 1
    }

    private fun luaToString(lua: Lua): Int {
        if (lua.isNil(1)) {
            lua.push("nil")
            return 1
        }

        when (lua.type(1)) {
            Lua.LuaType.TABLE -> {
                lua.pushNil()
                val sb = StringBuilder("{")
                while (lua.next(-2) != 0) {
                    if (sb.length > 1) {
                        sb.append(", ")
                    }

                    lua.getGlobal("tostring")
                    lua.pushValue(-3)
                    lua.pCall(1, 1)
                    val key = lua.toString(-1).also { lua.pop(1) }

                    lua.getGlobal("tostring")
                    lua.pushValue(-2)
                    lua.pCall(1, 1)
                    val value = lua.toString(-1).also { lua.pop(1) }

                    sb.append(key)
                    sb.append(" = ")
                    sb.append(value)
                    lua.pop(1)
                }
                sb.append("}")
                lua.push(sb.toString())
            }

            Lua.LuaType.USERDATA -> {
                lua.push(lua.checkUserdata(1, ManagedLuaTable::class).toString())
            }

            else -> lua.throwTypeError(1, Lua.LuaType.TABLE)
        }

        return 1
    }

}
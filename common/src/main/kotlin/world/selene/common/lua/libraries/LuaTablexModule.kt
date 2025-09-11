package world.selene.common.lua.libraries

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.observable.ObservableMap
import world.selene.common.lua.util.checkType
import world.selene.common.lua.util.checkUserdata
import world.selene.common.lua.util.register
import world.selene.common.lua.util.throwTypeError
import world.selene.common.lua.util.toAnyMap

/**
 * Extended table manipulation functions beyond standard Lua table operations.
 * Registered as `tablex` global.
 */
@Suppress("SameReturnValue")
class LuaTablexModule : LuaModule {
    override val name: String = "tablex"
    override val registerAsGlobal: Boolean = true

    override fun register(table: LuaValue) {
        table.register("observable", this::luaObservable)
        table.register("find", this::luaFind)
        table.register("tostring", this::luaToString)
    }

    /**
     * Creates an observable map.
     * Observable maps consist of read-writable key-value pairs.
     * They are no longer considered a `table` and cannot be used in `table`-specific functions.
     *
     * ```signatures
     * observable() -> ObservableMap
     * observable(data: table) -> ObservableMap
     * ```
     */
    private fun luaObservable(lua: Lua): Int {
        val data = lua.toAnyMap(1) as MutableMap?
        lua.push(ObservableMap(data ?: mutableMapOf()), Lua.Conversion.NONE)
        return 1
    }

    /**
     * Finds the key of the first occurrence of a value in a table.
     * Returns the key if found, otherwise returns nil.
     *
     * ```signatures
     * find(tbl: table, value: any) -> any|nil
     * ```
     */
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

    /**
     * Converts a table to a string representation for debugging.
     * Handles nested tables and various data types.
     *
     * ```signatures
     * tostring(tbl: table|ObservableMap) -> string
     * ```
     */
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
                lua.push(lua.checkUserdata(1, ObservableMap::class).toString())
            }

            else -> lua.throwTypeError(1, Lua.LuaType.TABLE)
        }

        return 1
    }

}
package com.seleneworlds.server.sqlite

import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.toAny
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

/** SQLite databases and prepared SQL queries. */
class SqliteLuaApi(private val api: SqliteApi) : LuaModule {
    override val name = "selene.sqlite"

    override fun initialize(luaManager: LuaManager) {
        luaManager.defineMetatable(SqliteDatabase::class, databaseMetatable)
    }

    override fun register(table: LuaValue) {
        table.register("open", this::open)
    }

    private fun open(lua: Lua): Int {
        lua.push(api.open(lua.checkString(1)), Lua.Conversion.NONE)
        return 1
    }

    companion object {
        private fun execute(lua: Lua): Int {
            val database = lua.checkUserdata<SqliteDatabase>(1)
            lua.push(database.execute(lua.checkString(2), lua.parametersFrom(3)))
            return 1
        }

        private fun query(lua: Lua): Int {
            val database = lua.checkUserdata<SqliteDatabase>(1)
            lua.push(database.query(lua.checkString(2), lua.parametersFrom(3)), Lua.Conversion.FULL)
            return 1
        }

        private fun scalar(lua: Lua): Int {
            val database = lua.checkUserdata<SqliteDatabase>(1)
            lua.push(database.scalar(lua.checkString(2), lua.parametersFrom(3)), Lua.Conversion.FULL)
            return 1
        }

        private fun close(lua: Lua): Int {
            lua.checkUserdata<SqliteDatabase>(1).close()
            return 0
        }

        private fun isOpen(lua: Lua): Int {
            lua.push(lua.checkUserdata<SqliteDatabase>(1).isOpen)
            return 1
        }

        private fun Lua.parametersFrom(firstIndex: Int): List<Any?> {
            if (top < firstIndex) return emptyList()
            val first = toAny(firstIndex)
            if (top == firstIndex && first is List<*>) return first
            return (firstIndex..top).map(::toAny)
        }

        val databaseMetatable = LuaMappedMetatable(SqliteDatabase::class) {
            callable(::execute)
            callable(::query)
            callable(::scalar)
            callable(::close)
            getter(::isOpen)
        }
    }
}

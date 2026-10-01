package com.seleneworlds.common.lua

import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register
import org.slf4j.Logger
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

/** Writes messages from Lua scripts to the server log. */
class LoggingLuaApi(private val logger: Logger) : LuaModule {
    override val name = "selene.logging"

    override fun register(table: LuaValue) {
        table.register("trace") { lua -> log(lua, logger::trace) }
        table.register("debug") { lua -> log(lua, logger::debug) }
        table.register("info") { lua -> log(lua, logger::info) }
        table.register("warn") { lua -> log(lua, logger::warn) }
        table.register("error") { lua -> log(lua, logger::error) }
    }

    private fun log(lua: Lua, write: (String) -> Unit): Int {
        write(lua.checkString(1))
        return 0
    }
}

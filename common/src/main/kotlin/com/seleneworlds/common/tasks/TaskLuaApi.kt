package com.seleneworlds.common.tasks

import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkFunction
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.runCoroutine
import com.seleneworlds.common.lua.util.toAny
import com.seleneworlds.common.threading.MainThreadDispatcher
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class TaskLuaApi(
    private val mainThreadDispatcher: MainThreadDispatcher
) : LuaModule {
    override val name = "selene.task"

    override fun register(table: LuaValue) {
        table.register("Launch", ::luaLaunch)
    }

    private fun luaLaunch(lua: Lua): Int {
        val callback = lua.checkFunction(1)
        val args = Array(lua.top - 1) { index ->
            lua.toAny(index + 2)
        }
        callback.runCoroutine(mainThreadDispatcher, lua.getCallerInfo(), *args)
        return 0
    }
}

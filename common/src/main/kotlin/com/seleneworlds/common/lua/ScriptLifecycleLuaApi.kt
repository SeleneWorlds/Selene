package com.seleneworlds.common.lua

import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.util.checkFunction
import com.seleneworlds.common.lua.util.register
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

/** Cleanup for Lua registrations owned by a module or entrypoint. */
class ScriptLifecycleLuaApi(private val luaPackage: LuaPackageModule) : LuaModule {
    override val name = "selene.lifecycle"

    override fun register(table: LuaValue) {
        table.register("onUnload", this::onUnload)
    }

    private fun onUnload(lua: Lua): Int {
        val callback = lua.checkFunction(1)
        luaPackage.onScriptUnload {
            lua.push(callback)
            lua.pCall(0, 0)
        }
        return 0
    }
}

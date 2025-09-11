package world.selene.common.lua.libraries

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule

/**
 * Restricted set of functions from the Lua debug library.
 * Registered as `debug` global.
 *
 * Only the functions `traceback` and `getinfo` are available.
 */
class LuaDebugModule : LuaModule {
    override val name: String = "debug"
    override val registerAsGlobal: Boolean = true

    lateinit var debugTraceback: LuaValue
    lateinit var debugGetInfo: LuaValue

    override fun initialize(luaManager: LuaManager) {
        val lua = luaManager.lua
        lua.openLibrary("debug")
        val os = lua.get("debug")
        debugTraceback = os.get("traceback")
        debugGetInfo = os.get("getinfo")
        lua.set("debug", null)
    }

    override fun register(table: LuaValue) {
        table.set("traceback", debugTraceback)
        table.set("getinfo", debugGetInfo)
    }
}
package world.selene.common.lua.libraries

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule

/**
 * Restricted set of functions from the Lua os library.
 * Registered as `os` global.
 *
 * Only the functions `date`, `time` and `clock` are available.
 */
class LuaOsModule : LuaModule {
    override val name: String = "os"
    override val registerAsGlobal: Boolean = true

    lateinit var osDate: LuaValue
    lateinit var osTime: LuaValue
    lateinit var osClock: LuaValue

    override fun initialize(luaManager: LuaManager) {
        val lua = luaManager.lua
        lua.openLibrary("os")
        val os = lua.get("os")
        osDate = os.get("date")
        osTime = os.get("time")
        osClock = os.get("clock")
        lua.set("os", null)
    }

    override fun register(table: LuaValue) {
        table.set("date", osDate)
        table.set("time", osTime)
        table.set("clock", osClock)
    }

}
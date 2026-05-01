package world.selene.common.bundles

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.register

/**
 * Lookup bundle resource files.
 */
@Suppress("SameReturnValue")
class ResourcesLuaApi(private val api: ResourcesApi) : LuaModule {
    override val name = "selene.resources"

    override fun register(table: LuaValue) {
        table.register("ListFiles", this::luaListFiles)
        table.register("LoadAsString", this::luaLoadAsString)
        table.register("FileExists", this::luaFileExists)
    }

    private fun luaListFiles(lua: Lua): Int {
        lua.push(api.listFiles(lua.checkString(1), lua.checkString(2)), Lua.Conversion.FULL)
        return 1
    }

    private fun luaLoadAsString(lua: Lua): Int {
        lua.push(api.loadAsString(lua.checkString(1)))
        return 1
    }

    private fun luaFileExists(lua: Lua): Int {
        lua.push(api.fileExists(lua.checkString(1)))
        return 1
    }
}

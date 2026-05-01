package world.selene.server.maps

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register

/**
 * Create new map layer trees.
 */
@Suppress("SameReturnValue")
class ServerMapLuaApi(private val api: ServerMapApi) : LuaModule {
    override val name = "selene.map"

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreate)
    }

    private fun luaCreate(lua: Lua): Int {
        lua.push(api.create(), Lua.Conversion.NONE)
        return 1
    }
}

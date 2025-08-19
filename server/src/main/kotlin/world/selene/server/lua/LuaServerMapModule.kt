package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.register
import world.selene.server.data.Registries
import world.selene.server.maps.MapTree

class LuaServerMapModule(private val registries: Registries) : LuaModule {
    override val name = "selene.map"

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreate)
    }

    private fun luaCreate(lua: Lua): Int {
        val mapTree = MapTree(registries)
        lua.push(mapTree, Lua.Conversion.NONE)
        return 1
    }
}

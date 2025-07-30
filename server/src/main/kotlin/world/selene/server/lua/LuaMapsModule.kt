package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.register
import world.selene.server.maps.MapManager
import world.selene.server.maps.MapTree
import world.selene.server.sync.ScopedChunkView

class LuaMapsModule(private val mapManager: MapManager) : LuaModule {
    override val name = "selene.maps"

    override fun register(table: LuaValue) {
        table.register("Create", this::luaCreate)
    }

    override fun initialize(luaManager: LuaManager) {
        luaManager.exposeClass(MapTree.MapTreeLuaProxy::class)
        luaManager.exposeClass(ScopedChunkView.ScopedChunkViewLuaProxy::class)
    }

    private fun luaCreate(lua: Lua): Int {
        val mapTree = mapManager.createMapTree()
        lua.push(mapTree.luaProxy, Lua.Conversion.NONE)
        return 1
    }
}

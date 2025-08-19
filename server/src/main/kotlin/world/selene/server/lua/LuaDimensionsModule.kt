package world.selene.server.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.register
import world.selene.server.dimensions.Dimension
import world.selene.server.dimensions.DimensionManager
import world.selene.server.maps.TransientTile

class LuaDimensionsModule(private val dimensionManager: DimensionManager) : LuaModule {
    override val name = "selene.dimensions"

    override fun initialize(luaManager: LuaManager) {
        luaManager.exposeClass(TransientTile::class)
    }

    override fun register(table: LuaValue) {
        table.register("GetDefault", this::luaGetDefault)
    }

    private fun luaGetDefault(lua: Lua): Int {
        lua.push(dimensionManager.dimensions[0]!!, Lua.Conversion.NONE)
        return 1
    }
}

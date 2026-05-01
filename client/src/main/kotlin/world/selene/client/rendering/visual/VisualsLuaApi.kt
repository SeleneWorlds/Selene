package world.selene.client.rendering.visual

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register

/**
 * Create visuals from visual definitions.
 */
class VisualsLuaApi(private val api: VisualsApi) : LuaModule {
    override val name = "selene.visuals"

    override fun register(table: LuaValue) {
        table.register("Create", api::luaCreate)
    }
}

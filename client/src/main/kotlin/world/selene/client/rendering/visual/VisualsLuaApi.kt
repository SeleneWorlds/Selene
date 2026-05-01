package world.selene.client.rendering.visual

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkIdentifier
import world.selene.common.lua.util.throwError
import world.selene.common.lua.util.register

/**
 * Create visuals from visual definitions.
 */
class VisualsLuaApi(private val api: VisualsApi) : LuaModule {
    override val name = "selene.visuals"

    override fun register(table: LuaValue) {
        table.register("Create", ::luaCreate)
    }

    private fun luaCreate(lua: Lua): Int {
        val identifier = lua.checkIdentifier(1)
        try {
            lua.push(api.create(identifier), Lua.Conversion.NONE)
            return 1
        } catch (e: IllegalArgumentException) {
            return lua.throwError(e.message ?: "Unable to create visual")
        }
    }
}

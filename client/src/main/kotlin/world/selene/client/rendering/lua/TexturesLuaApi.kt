package world.selene.client.rendering.lua

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register

/**
 * Create and manipulate textures.
 */
class TexturesLuaApi(private val api: TexturesApi) : LuaModule {
    override val name = "selene.textures"

    override fun register(table: LuaValue) {
        table.register("Create", api::luaCreateTexture)
    }
}

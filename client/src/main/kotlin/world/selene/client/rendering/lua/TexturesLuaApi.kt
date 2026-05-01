package world.selene.client.rendering.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkInt
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.register

/**
 * Create and manipulate textures.
 */
class TexturesLuaApi(private val api: TexturesApi) : LuaModule {
    override val name = "selene.textures"

    override fun initialize(luaManager: LuaManager) {
        luaManager.defineMetatable(ScriptableTexture::class, ScriptableTexture.luaMeta)
    }

    override fun register(table: LuaValue) {
        table.register("Create", ::luaCreateTexture)
    }

    private fun luaCreateTexture(lua: Lua): Int {
        val width = lua.checkInt(1)
        val height = lua.checkInt(2)
        val formatName = if (lua.top >= 3) lua.checkString(3) else "RGBA8888"
        lua.push(api.createTexture(width, height, formatName), Lua.Conversion.NONE)
        return 1
    }
}

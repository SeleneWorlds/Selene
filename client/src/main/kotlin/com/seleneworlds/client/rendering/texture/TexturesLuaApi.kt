package com.seleneworlds.client.rendering.texture

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkInt
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.register

/**
 * Create and manipulate textures.
 */
class TexturesLuaApi(private val api: TexturesApi) : LuaModule {
    override val name = "selene.textures"

    override fun initialize(luaManager: LuaManager) {
        luaManager.defineMetatable(ScriptableTexture::class, ScriptableTextureLuaApi.luaMeta)
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
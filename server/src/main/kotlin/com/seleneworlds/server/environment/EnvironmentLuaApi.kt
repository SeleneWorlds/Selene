package com.seleneworlds.server.environment

import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkType
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.getFieldFloat
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.server.players.PlayerApi
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

class EnvironmentLuaApi(private val api: EnvironmentApi) : LuaModule {
    override val name = "selene.environment"

    override fun register(table: LuaValue) {
        table.register("setAmbientLight", ::setAmbientLight)
        table.register("setGlobalAmbientLight", ::setGlobalAmbientLight)
    }

    private fun setAmbientLight(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        val color = color(lua, 2)
        api.setAmbientLight(player, color.first, color.second, color.third)
        return 0
    }

    private fun setGlobalAmbientLight(lua: Lua): Int {
        val color = color(lua, 1)
        api.setGlobalAmbientLight(color.first, color.second, color.third)
        return 0
    }

    private fun color(lua: Lua, index: Int): Triple<Float, Float, Float> {
        lua.checkType(index, Lua.LuaType.TABLE)
        return Triple(
            lua.getFieldFloat(index, "red") ?: lua.getFieldFloat(index, "r") ?: 1f,
            lua.getFieldFloat(index, "green") ?: lua.getFieldFloat(index, "g") ?: 1f,
            lua.getFieldFloat(index, "blue") ?: lua.getFieldFloat(index, "b") ?: 1f,
        )
    }
}

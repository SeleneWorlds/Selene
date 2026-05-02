package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.Stage
import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkFloat
import com.seleneworlds.common.lua.util.checkUserdata

object StageLuaMetatable {
    val luaMeta = LuaMappedMetatable(Stage::class) {
        callable(::luaHit)
    }

    private fun luaHit(lua: Lua): Int {
        val stage = lua.checkUserdata<Stage>(1)
        val stageX = lua.checkFloat(2)
        val stageY = lua.checkFloat(3)
        val touchable = if(lua.isBoolean(4)) lua.toBoolean(4) else true
        val actor = stage.hit(stageX, stageY, touchable)
        lua.push(actor, Lua.Conversion.NONE)
        return 1
    }
}
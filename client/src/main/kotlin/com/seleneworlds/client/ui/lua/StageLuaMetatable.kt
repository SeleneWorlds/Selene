package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkFloat
import com.seleneworlds.common.lua.util.checkUserdata

object StageLuaMetatable {
    val luaMeta = LuaMappedMetatable(Stage::class) {
        callable(::luaHit)
        callable(::luaScreenToStage)
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

    private fun luaScreenToStage(lua: Lua): Int {
        val stage = lua.checkUserdata<Stage>(1)
        val screenX = lua.checkFloat(2)
        val screenY = lua.checkFloat(3)
        val stageCoords = stage.viewport.unproject(Vector3(screenX, screenY, 0f))
        lua.push(stageCoords.x)
        lua.push(stageCoords.y)
        return 2
    }
}
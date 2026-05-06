package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.util.checkFloat
import com.seleneworlds.common.lua.util.checkUserdata

object ProgressBarLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(ProgressBar::class) {
        callable(::getValue)
        callable(::setValue)
    }

    private fun getValue(lua: Lua): Int {
        val progressBar = lua.checkUserdata<ProgressBar>(1)
        lua.push(progressBar.value)
        return 1
    }

    private fun setValue(lua: Lua): Int {
        val progressBar = lua.checkUserdata<ProgressBar>(1)
        val value = lua.checkFloat(2)
        progressBar.setValue(value)
        return 0
    }
}

package world.selene.client.lua

import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import party.iroiro.luajava.Lua
import world.selene.common.lua.checkFloat
import world.selene.common.lua.checkUserdata

object ProgressBarLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(ProgressBar::class) {
        getter(::luaGetValue)
        setter(::luaSetValue)
    }

    private fun luaGetValue(lua: Lua): Int {
        val progressBar = lua.checkUserdata<ProgressBar>(1)
        lua.push(progressBar.value)
        return 1
    }

    private fun luaSetValue(lua: Lua): Int {
        val progressBar = lua.checkUserdata<ProgressBar>(1)
        val value = lua.checkFloat(3)
        progressBar.setValue(value)
        return 0
    }
}
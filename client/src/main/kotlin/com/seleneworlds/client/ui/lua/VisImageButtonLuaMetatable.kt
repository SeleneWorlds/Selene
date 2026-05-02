package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.kotcrab.vis.ui.widget.VisImageButton
import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata

object VisImageButtonLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(VisImageButton::class) {
        callable(::luaSetStyle)
    }

    private fun luaSetStyle(lua: Lua): Int {
        val actor = lua.checkUserdata<VisImageButton>(1)
        val skin = lua.checkUserdata<Skin>(2)
        val style = lua.checkString(3)
        actor.style = skin.get(style, VisImageButton.VisImageButtonStyle::class.java)
        return 0
    }
}
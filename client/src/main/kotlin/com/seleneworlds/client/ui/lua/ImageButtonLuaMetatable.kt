package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import party.iroiro.luajava.Lua
import com.seleneworlds.client.ui.ThemeApi
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata

object ImageButtonLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(ImageButton::class) {
        callable(::luaSetStyle)
    }

    private fun luaSetStyle(lua: Lua): Int {
        val actor = lua.checkUserdata<ImageButton>(1)
        val skinOrStyle = lua.toJavaObject(2)
        actor.style = if (skinOrStyle is ThemeApi) {
            val style = lua.checkString(3)
            skinOrStyle.skin.get(style, ImageButton.ImageButtonStyle::class.java)
        } else skinOrStyle as? ImageButton.ImageButtonStyle
            ?: return lua.error(IllegalArgumentException("Expected ThemeApi or ImageButtonStyle"))
        return 0
    }
}

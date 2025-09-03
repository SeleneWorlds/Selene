package world.selene.client.lua

import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import party.iroiro.luajava.Lua
import world.selene.common.lua.checkString
import world.selene.common.lua.checkUserdata

object ImageButtonLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(ImageButton::class) {
        callable(::luaSetStyle)
    }

    private fun luaSetStyle(lua: Lua): Int {
        val actor = lua.checkUserdata<ImageButton>(1)
        val skinOrStyle = lua.toJavaObject(2)
        actor.style = if (skinOrStyle is Skin) {
            val style = lua.checkString(3)
            skinOrStyle.get(style, ImageButton.ImageButtonStyle::class.java)
        } else skinOrStyle as? ImageButton.ImageButtonStyle
            ?: return lua.error(IllegalArgumentException("Expected Skin or ImageButtonStyle"))
        return 0
    }
}
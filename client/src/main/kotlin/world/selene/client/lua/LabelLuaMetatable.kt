package world.selene.client.lua

import com.badlogic.gdx.scenes.scene2d.ui.Label
import party.iroiro.luajava.Lua
import world.selene.common.lua.checkString
import world.selene.common.lua.checkUserdata

@Suppress("SameReturnValue")
object LabelLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(Label::class) {
        getter(::luaGetText)
        setter(::luaSetText)
    }

    private fun luaGetText(lua: Lua): Int {
        val label = lua.checkUserdata<Label>(1)
        lua.push(label.text.toString())
        return 1
    }

    private fun luaSetText(lua: Lua): Int {
        val label = lua.checkUserdata<Label>(1)
        val text = lua.checkString(3)
        label.setText(text)
        return 0
    }
}
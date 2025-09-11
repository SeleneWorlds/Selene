package world.selene.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.ui.TextField
import party.iroiro.luajava.Lua
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata

@Suppress("SameReturnValue")
object TextFieldLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(TextField::class) {
        getter(::luaGetInputListener)
        getter(::luaGetText)
        setter(::luaSetText)
    }

    private fun luaGetInputListener(lua: Lua): Int {
        val textField = lua.checkUserdata<TextField>(1)
        lua.push(textField.defaultInputListener, Lua.Conversion.NONE)
        return 1
    }

    private fun luaGetText(lua: Lua): Int {
        val textField = lua.checkUserdata<TextField>(1)
        lua.push(textField.text.toString())
        return 1
    }

    private fun luaSetText(lua: Lua): Int {
        val textField = lua.checkUserdata<TextField>(1)
        val text = lua.checkString(3)
        textField.setText(text)
        return 0
    }
}
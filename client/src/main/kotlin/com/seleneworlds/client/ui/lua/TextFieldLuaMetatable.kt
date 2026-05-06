package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.ui.TextField
import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata

object TextFieldLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(TextField::class) {
        callable(::getInputListener)
        callable(::getText)
        callable(::setText)
    }

    private fun getInputListener(lua: Lua): Int {
        val textField = lua.checkUserdata<TextField>(1)
        lua.push(textField.defaultInputListener, Lua.Conversion.NONE)
        return 1
    }

    private fun getText(lua: Lua): Int {
        val textField = lua.checkUserdata<TextField>(1)
        lua.push(textField.text.toString())
        return 1
    }

    private fun setText(lua: Lua): Int {
        val textField = lua.checkUserdata<TextField>(1)
        val text = lua.checkString(2)
        textField.setText(text)
        return 0
    }
}

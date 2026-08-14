package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.seleneworlds.common.lua.util.checkBoolean
import com.seleneworlds.common.lua.util.checkFunction
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.common.script.ConstantTrace
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

object CheckBoxLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(CheckBox::class) {
        callable(::isChecked)
        callable(::setChecked)
        callable(::onChanged)
    }

    private fun isChecked(lua: Lua): Int {
        val checkBox = lua.checkUserdata<CheckBox>(1)
        lua.push(checkBox.isChecked)
        return 1
    }

    private fun setChecked(lua: Lua): Int {
        val checkBox = lua.checkUserdata<CheckBox>(1)
        checkBox.isChecked = lua.checkBoolean(2)
        return 0
    }

    private fun onChanged(lua: Lua): Int {
        val checkBox = lua.checkUserdata<CheckBox>(1)
        val callback = lua.checkFunction(2)
        val trace = ConstantTrace("[checkBox onChanged] registered in ${lua.getCallerInfo()}")

        checkBox.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                runCallback(callback, trace, checkBox)
            }
        })
        return 0
    }

    private fun runCallback(callback: LuaValue, trace: ConstantTrace, checkBox: CheckBox) {
        val callbackLua = callback.state()
        callbackLua.push(callback)
        callbackLua.push(checkBox, Lua.Conversion.NONE)
        callbackLua.push(checkBox.isChecked)
        try {
            callbackLua.xpCall(2, 0, trace)
        } catch (e: LuaException) {
            logger.error("Lua error in check box change callback", e)
        }
    }

    private val logger = LoggerFactory.getLogger(CheckBoxLuaMetatable::class.java)
}

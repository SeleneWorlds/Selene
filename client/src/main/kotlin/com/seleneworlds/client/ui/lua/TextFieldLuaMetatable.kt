package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.seleneworlds.client.ui.BundleUiListeners
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.lua.util.checkFunction
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.common.script.ConstantTrace
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

object TextFieldLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(TextField::class) {
        callable(::getInputListener)
        callable(::getText)
        callable(::setText)
        callable(::getMessageText)
        callable(::setMessageText)
        callable(::onChanged)
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

    private fun getMessageText(lua: Lua): Int {
        val textField = lua.checkUserdata<TextField>(1)
        textField.messageText?.let(lua::push) ?: lua.pushNil()
        return 1
    }

    private fun setMessageText(lua: Lua): Int {
        val textField = lua.checkUserdata<TextField>(1)
        textField.messageText = if (lua.isNil(2)) null else lua.checkString(2)
        return 0
    }

    private fun onChanged(lua: Lua): Int {
        val textField = lua.checkUserdata<TextField>(1)
        val callback = lua.checkFunction(2)
        val trace = ConstantTrace("[textField onChanged] registered in ${lua.getCallerInfo()}")
        val bundle = BundleExecutionContext.currentBundle

        BundleUiListeners.addActorListener(textField, object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                BundleUiListeners.runInBundleContext(bundle) {
                    runCallback(callback, trace, textField)
                }
            }
        }, bundle)
        return 0
    }

    private fun runCallback(callback: LuaValue, trace: ConstantTrace, textField: TextField) {
        val callbackLua = callback.state()
        callbackLua.push(callback)
        callbackLua.push(textField, Lua.Conversion.NONE)
        callbackLua.push(textField.text)
        try {
            callbackLua.xpCall(2, 0, trace)
        } catch (e: LuaException) {
            logger.error("Lua error in text field change callback", e)
        }
    }

    private val logger = LoggerFactory.getLogger(TextFieldLuaMetatable::class.java)
}

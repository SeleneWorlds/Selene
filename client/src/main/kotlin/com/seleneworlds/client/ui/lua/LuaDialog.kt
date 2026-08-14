package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.common.script.ConstantTrace
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

class LuaDialog(
    title: String,
    skin: Skin
) : Dialog(title, skin) {
    private data class ButtonCallback(
        val text: String,
        val callback: LuaValue?,
        val trace: ConstantTrace
    )

    fun addCallbackButton(text: String, callback: LuaValue?, trace: ConstantTrace): LuaDialog {
        button(text, ButtonCallback(text, callback, trace))
        if (stage != null) {
            pack()
        }
        return this
    }

    override fun result(obj: Any?) {
        val buttonCallback = obj as? ButtonCallback ?: return
        val callback = buttonCallback.callback ?: return
        val callbackLua = callback.state()
        callbackLua.push(callback)
        callbackLua.push(this, Lua.Conversion.NONE)
        callbackLua.push(buttonCallback.text)
        try {
            callbackLua.xpCall(2, 0, buttonCallback.trace)
        } catch (e: LuaException) {
            logger.error("Lua error in dialog button callback {}", buttonCallback.text, e)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LuaDialog::class.java)
    }
}

package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.seleneworlds.client.ui.UIApi
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.toAny
import com.seleneworlds.common.lua.util.toFunction
import com.seleneworlds.common.script.ConstantTrace
import party.iroiro.luajava.Lua

class DialogLuaMetatable(private val api: UIApi) {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(Dialog::class) {
        callable(::open)
        callable(::close)
        callable(::setTitle)
        callable(::addText)
        callable(::addButton)
    }

    /**
     * Opens this dialog.
     *
     * ```signatures
     * open()
     * ```
    */
    private fun open(lua: Lua): Int {
        val dialog = lua.checkUserdata<Dialog>(1)
        api.openDialog(dialog)
        return 0
    }

    /**
     * Closes this dialog.
     *
     * ```signatures
     * close()
     * ```
     */
    private fun close(lua: Lua): Int {
        lua.checkUserdata<Dialog>(1).hide()
        return 0
    }

    /**
     * Sets the dialog title.
     *
     * ```signatures
     * setTitle(text: string)
     * ```
     */
    private fun setTitle(lua: Lua): Int {
        val dialog = lua.checkUserdata<Dialog>(1)
        dialog.titleLabel.setText(lua.checkString(2))
        dialog.invalidateHierarchy()
        if (dialog.stage != null) {
            dialog.pack()
        }
        return 0
    }

    /**
     * Adds text to the dialog content table.
     *
     * ```signatures
     * addText(text: string)
     * ```
     */
    private fun addText(lua: Lua): Int {
        val dialog = lua.checkUserdata<Dialog>(1)
        dialog.text(lua.checkString(2))
        dialog.invalidateHierarchy()
        if (dialog.stage != null) {
            dialog.pack()
        }
        return 0
    }

    /**
     * Adds a button. Callback is only supported on script-managed dialogs, receiving `(dialog, buttonText)`.
     *
     * ```signatures
     * addButton(text: string, callback: function|nil)
     * addButton(text: string, value: any)
     * ```
     */
    private fun addButton(lua: Lua): Int {
        val dialog = lua.checkUserdata<Dialog>(1)
        val text = lua.checkString(2)
        if (dialog is LuaDialog) {
            dialog.addCallbackButton(
                text = text,
                callback = lua.toFunction(3),
                trace = ConstantTrace("[dialog button \"$text\"] registered in ${lua.getCallerInfo()}")
            )
        } else {
            dialog.button(text, lua.toAny(3))
            if (dialog.stage != null) {
                dialog.pack()
            }
        }
        return 0
    }
}

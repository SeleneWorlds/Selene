package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.seleneworlds.common.lua.util.checkBoolean
import com.seleneworlds.common.lua.util.checkUserdata
import party.iroiro.luajava.Lua

object ButtonLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(Button::class) {
        callable(::setDisabled)
        callable(::isDisabled)
        callable(::setChecked)
        callable(::isChecked)
    }

    /**
     * Enables or disables this button.
     *
     * ```signatures
     * setDisabled(disabled: boolean)
     * ```
     */
    private fun setDisabled(lua: Lua): Int {
        val button = lua.checkUserdata<Button>(1)
        button.isDisabled = lua.checkBoolean(2)
        return 0
    }

    /**
     * Whether this button is currently disabled.
     *
     * ```signatures
     * isDisabled(): boolean
     * ```
     */
    private fun isDisabled(lua: Lua): Int {
        val button = lua.checkUserdata<Button>(1)
        lua.push(button.isDisabled)
        return 1
    }

    /**
     * Sets the checked state for toggle-style buttons, including buttons in button groups.
     *
     * ```signatures
     * setChecked(checked: boolean)
     * ```
     */
    private fun setChecked(lua: Lua): Int {
        val button = lua.checkUserdata<Button>(1)
        button.isChecked = lua.checkBoolean(2)
        return 0
    }

    /**
     * Whether this button is currently checked.
     *
     * ```signatures
     * isChecked(): boolean
     * ```
     */
    private fun isChecked(lua: Lua): Int {
        val button = lua.checkUserdata<Button>(1)
        lua.push(button.isChecked)
        return 1
    }
}

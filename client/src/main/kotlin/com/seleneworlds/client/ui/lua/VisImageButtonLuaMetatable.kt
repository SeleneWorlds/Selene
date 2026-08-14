package com.seleneworlds.client.ui.lua

import com.kotcrab.vis.ui.widget.VisImageButton
import party.iroiro.luajava.Lua
import com.seleneworlds.client.ui.ThemeApi
import com.seleneworlds.client.ui.UIApi
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata

class VisImageButtonLuaMetatable(private val api: UIApi) {
    val luaMeta = ButtonLuaMetatable.luaMeta.extend(VisImageButton::class) {
        callable(::setStyle)
        callable(::setImageDrawable)
    }

    private fun setStyle(lua: Lua): Int {
        val actor = lua.checkUserdata<VisImageButton>(1)
        val theme = lua.checkUserdata<ThemeApi>(2)
        val style = lua.checkString(3)
        actor.style = theme.skin.get(style, VisImageButton.VisImageButtonStyle::class.java)
        return 0
    }

    /**
     * Sets an image-button icon drawable from a theme key, Visual2D, or Drawable.
     *
     * ```signatures
     * SetImageDrawable(drawable: Visual2D|Drawable, state: string|nil)
     * SetImageDrawable(theme: ThemeApi, drawableKey: string, state: string|nil)
     * ```
     */
    private fun setImageDrawable(lua: Lua): Int {
        val actor = lua.checkUserdata<VisImageButton>(1)
        val hasThemeKey = lua.isUserdata(2) && lua.isString(3)
        val drawable = if (hasThemeKey) {
            lua.checkDrawable(api, 2, 3)
        } else {
            lua.checkDrawable(2)
        }
        val state = if (hasThemeKey) {
            if (lua.top >= 4) lua.checkString(4) else "up"
        } else {
            if (lua.top >= 3) lua.checkString(3) else "up"
        }
        actor.style = VisImageButton.VisImageButtonStyle(actor.style).apply {
            when (state) {
                "up", "imageUp" -> imageUp = drawable
                "down", "imageDown" -> imageDown = drawable
                "checked", "imageChecked" -> imageChecked = drawable
                "over", "imageOver" -> imageOver = drawable
                "checkedOver", "imageCheckedOver" -> imageCheckedOver = drawable
                "disabled", "imageDisabled" -> imageDisabled = drawable
                else -> throw IllegalArgumentException("Unknown image button drawable state: $state")
            }
        }
        return 0
    }
}

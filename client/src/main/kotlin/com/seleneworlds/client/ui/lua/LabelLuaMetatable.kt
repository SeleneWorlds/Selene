package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Align
import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.util.checkBoolean
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.throwError

object LabelLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(Label::class) {
        callable(::getText)
        callable(::setText)
        callable(::setWrap)
        callable(::setAlignment)
    }

    private fun getText(lua: Lua): Int {
        val label = lua.checkUserdata<Label>(1)
        lua.push(label.text.toString())
        return 1
    }

    private fun setText(lua: Lua): Int {
        val label = lua.checkUserdata<Label>(1)
        val text = lua.checkString(2)
        label.setText(text)
        return 0
    }

    /**
     * ```signatures
     * SetWrap(wrap: boolean)
     * ```
     */
    private fun setWrap(lua: Lua): Int {
        val label = lua.checkUserdata<Label>(1)
        label.setWrap(lua.checkBoolean(2))
        return 0
    }

    /**
     * ```signatures
     * SetAlignment(alignment: string, lineAlignment: string|nil)
     * ```
     */
    private fun setAlignment(lua: Lua): Int {
        val label = lua.checkUserdata<Label>(1)
        val labelAlignment = lua.checkAlignment(2)
        if (lua.isString(3)) {
            label.setAlignment(labelAlignment, lua.checkLineAlignment(3))
        } else {
            label.setAlignment(labelAlignment)
        }
        return 0
    }

    private fun Lua.checkAlignment(index: Int): Int {
        return when (val alignment = checkString(index).normalizedAlignmentName()) {
            "topleft", "lefttop" -> Align.topLeft
            "top" -> Align.top
            "topright", "righttop" -> Align.topRight
            "left" -> Align.left
            "center", "centre" -> Align.center
            "right" -> Align.right
            "bottomleft", "leftbottom" -> Align.bottomLeft
            "bottom" -> Align.bottom
            "bottomright", "rightbottom" -> Align.bottomRight
            else -> throwError("Unknown label alignment '$alignment'")
        }
    }

    private fun Lua.checkLineAlignment(index: Int): Int {
        return when (val alignment = checkString(index).normalizedAlignmentName()) {
            "left" -> Align.left
            "center", "centre" -> Align.center
            "right" -> Align.right
            else -> throwError("Unknown label line alignment '$alignment'")
        }
    }

    private fun String.normalizedAlignmentName(): String {
        return lowercase().filter(Char::isLetter)
    }
}

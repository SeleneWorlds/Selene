package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.kotcrab.vis.ui.widget.VisTextField
import party.iroiro.luajava.Lua
import com.seleneworlds.client.rendering.texture.ScriptableTexture
import com.seleneworlds.client.ui.ThemeApi
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkType
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.getFieldString

class ThemeLuaApi(private val luaApi: UILuaApi) {
    val luaMeta = LuaMappedMetatable(ThemeApi::class) {
        callable(::luaAddTexture)
        callable(::luaAddButtonStyle)
        callable(::luaAddLabelStyle)
        callable(::luaAddImageButtonStyle)
        callable(::luaAddTextFieldStyle)
        callable(::luaAddScrollPaneStyle)
        callable(::luaAddProgressBarStyle)
    }

    /**
     * Adds a texture to the theme by name and path or LuaTexture object.
     * Returns a future so Lua coroutines can yield on it.
     *
     * ```signatures
     * AddTexture(name: string, texturePath: string)
     * AddTexture(name: string, texture: LuaTexture)
     * ```
     */
    private fun luaAddTexture(lua: Lua): Int {
        val theme = lua.checkUserdata<ThemeApi>(1)
        val name = lua.checkString(2)
        if (lua.isString(3)) {
            lua.push(theme.addTexture(name, lua.checkString(3)), Lua.Conversion.NONE)
            return 1
        } else if (lua.isUserdata(3)) {
            lua.push(theme.addTexture(name, lua.checkUserdata<ScriptableTexture>(3)), Lua.Conversion.NONE)
            return 1
        }
        return lua.error(IllegalArgumentException("Expected texture path string or ScriptableTexture for AddTexture"))
    }

    /**
     * Adds a button style to the theme.
     *
     * ```signatures
     * AddButtonStyle(name: string, config: table{up: string|Visual2D|Drawable, down: string|Visual2D|Drawable, checked: string|Visual2D|Drawable, over: string|Visual2D|Drawable, focused: string|Visual2D|Drawable, disabled: string|Visual2D|Drawable, checkedOver: string|Visual2D|Drawable, checkedDown: string|Visual2D|Drawable, checkedFocused: string|Visual2D|Drawable, checkedOffsetX: number, checkedOffsetY: number, pressedOffsetX: number, pressedOffsetY: number, unpressedOffsetX: number, unpressedOffsetY: number})
     * ```
     */
    private fun luaAddButtonStyle(lua: Lua): Int {
        val theme = lua.checkUserdata<ThemeApi>(1)
        val styleName = lua.checkString(2)
        val styles = luaApi.createButtonStyle(lua, 3, theme)
        for (style in styles) {
            theme.skin.add(styleName, style)
        }
        return 0
    }

    /**
     * Adds a label style to the theme.
     * Font colors can be hex strings or color names. Background is optional.
     *
     * ```signatures
     * AddLabelStyle(name: string, config: table{font: string, fontColor: string, background: string})
     * ```
     */
    private fun luaAddLabelStyle(lua: Lua): Int {
        val theme = lua.checkUserdata<ThemeApi>(1)
        val styleName = lua.checkString(2)
        lua.checkType(3, Lua.LuaType.TABLE)

        val font = lua.getFieldString(3, "font")?.let {
            luaApi.api.skinResolvers.resolveFont(theme.skin, it)
        }
        val fontColor = lua.getFieldString(3, "fontColor")?.let {
            luaApi.api.skinResolvers.resolveColor(theme.skin, it)
        } ?: Color.WHITE
        val labelStyle = Label.LabelStyle(font, fontColor)
        lua.getFieldString(3, "background")?.let {
            labelStyle.background = luaApi.api.skinResolvers.resolveDrawable(theme, it)
        }
        theme.skin.add(styleName, labelStyle)
        return 0
    }

    /**
     * Adds an image button style to the theme.
     *
     * ```signatures
     * AddImageButtonStyle(name: string, config: table{up: string|Visual2D|Drawable, down: string|Visual2D|Drawable, over: string|Visual2D|Drawable, imageUp: string|Visual2D|Drawable, imageDown: string|Visual2D|Drawable, imageOver: string|Visual2D|Drawable})
     * ```
     */
    private fun luaAddImageButtonStyle(lua: Lua): Int {
        val theme = lua.checkUserdata<ThemeApi>(1)
        val styleName = lua.checkString(2)
        val styles = luaApi.createImageButtonStyle(lua, 3, theme)
        for (style in styles) {
            theme.skin.add(styleName, style)
        }
        return 0
    }

    /**
     * Adds a text field style to the theme.
     * Creates both regular TextField and VisTextField styles. Supports focused and disabled states.
     *
     * ```signatures
     * AddTextFieldStyle(name: string, config: table{font: string, fontColor: string, cursor: string, selection: string, background: string, focusedFontColor: string, disabledFontColor: string, focusedBackground: string, disabledBackground: string, messageFont: string, messageFontColor: string})
     * ```
     */
    private fun luaAddTextFieldStyle(lua: Lua): Int {
        val theme = lua.checkUserdata<ThemeApi>(1)
        val styleName = lua.checkString(2)
        lua.checkType(3, Lua.LuaType.TABLE)

        val font = lua.getFieldString(3, "font")?.let {
            luaApi.api.skinResolvers.resolveFont(theme.skin, it)
        }
        val fontColor = lua.getFieldString(3, "fontColor")?.let {
            luaApi.api.skinResolvers.resolveColor(theme.skin, it)
        } ?: Color.WHITE
        val cursor = lua.getFieldString(3, "cursor")?.let {
            luaApi.api.skinResolvers.resolveDrawable(theme, it)
        }
        val selection = lua.getFieldString(3, "selection")?.let {
            luaApi.api.skinResolvers.resolveDrawable(theme, it)
        }
        val background = lua.getFieldString(3, "background")?.let {
            luaApi.api.skinResolvers.resolveDrawable(theme, it)
        }
        val textFieldStyle = TextField.TextFieldStyle(font, fontColor, cursor, selection, background)
        val visTextFieldStyle = VisTextField.VisTextFieldStyle(font, fontColor, cursor, selection, background)

        lua.getFieldString(3, "focusedFontColor")?.let {
            val color = luaApi.api.skinResolvers.resolveColor(theme.skin, it)
            textFieldStyle.focusedFontColor = color
            visTextFieldStyle.focusedFontColor = color
        }

        lua.getFieldString(3, "disabledFontColor")?.let {
            val color = luaApi.api.skinResolvers.resolveColor(theme.skin, it)
            textFieldStyle.disabledFontColor = color
            visTextFieldStyle.disabledFontColor = color
        }

        lua.getFieldString(3, "focusedBackground")?.let {
            val drawable = luaApi.api.skinResolvers.resolveDrawable(theme, it)
            textFieldStyle.focusedBackground = drawable
            visTextFieldStyle.focusedBackground = drawable
        }

        lua.getFieldString(3, "disabledBackground")?.let {
            val drawable = luaApi.api.skinResolvers.resolveDrawable(theme, it)
            textFieldStyle.disabledBackground = drawable
            visTextFieldStyle.disabledBackground = drawable
        }

        lua.getFieldString(3, "messageFont")?.let {
            val font = luaApi.api.skinResolvers.resolveFont(theme.skin, it)
            textFieldStyle.messageFont = font
            visTextFieldStyle.messageFont = font
        }

        lua.getFieldString(3, "messageFontColor")?.let {
            val color = luaApi.api.skinResolvers.resolveColor(theme.skin, it)
            textFieldStyle.messageFontColor = color
            visTextFieldStyle.messageFontColor = color
        }

        theme.skin.add(styleName, textFieldStyle)
        return 0
    }

    /**
     * Adds a scroll pane style to the theme.
     * All drawable properties are optional and will be resolved using the theme.
     *
     * ```signatures
     * AddScrollPaneStyle(name: string, config: table{background: string, hScroll: string, hScrollKnob: string, vScroll: string, vScrollKnob: string, corner: string})
     * ```
     */
    private fun luaAddScrollPaneStyle(lua: Lua): Int {
        val theme = lua.checkUserdata<ThemeApi>(1)
        val styleName = lua.checkString(2)
        lua.checkType(3, Lua.LuaType.TABLE)

        val background = lua.getFieldString(3, "background")?.let {
            luaApi.api.skinResolvers.resolveDrawable(theme, it)
        }
        val hScroll = lua.getFieldString(3, "hScroll")?.let {
            luaApi.api.skinResolvers.resolveDrawable(theme, it)
        }
        val hScrollKnob = lua.getFieldString(3, "hScrollKnob")?.let {
            luaApi.api.skinResolvers.resolveDrawable(theme, it)
        }
        val vScroll = lua.getFieldString(3, "vScroll")?.let {
            luaApi.api.skinResolvers.resolveDrawable(theme, it)
        }
        val vScrollKnob = lua.getFieldString(3, "vScrollKnob")?.let {
            luaApi.api.skinResolvers.resolveDrawable(theme, it)
        }
        val scrollPaneStyle = ScrollPane.ScrollPaneStyle(background, hScroll, hScrollKnob, vScroll, vScrollKnob)
        lua.getFieldString(3, "corner")?.let {
            val drawable = luaApi.api.skinResolvers.resolveDrawable(theme, it)
            scrollPaneStyle.corner = drawable
        }
        theme.skin.add(styleName, scrollPaneStyle)
        return 0
    }

    /**
     * Adds a progress bar style to the theme.
     * Supports both normal and disabled states for all drawable properties.
     *
     * ```signatures
     * AddProgressBarStyle(name: string, config: table{background: string, knob: string, knobBefore: string, knobAfter: string, disabledBackground: string, disabledKnob: string, disabledKnobBefore: string, disabledKnobAfter: string})
     * ```
     */
    private fun luaAddProgressBarStyle(lua: Lua): Int {
        val theme = lua.checkUserdata<ThemeApi>(1)
        val styleName = lua.checkString(2)
        lua.checkType(3, Lua.LuaType.TABLE)

        val background = lua.getFieldString(3, "background")?.let {
            luaApi.api.skinResolvers.resolveDrawable(theme, it)
        }
        val knob = lua.getFieldString(3, "knob")?.let {
            luaApi.api.skinResolvers.resolveDrawable(theme, it)
        }
        val progressBarStyle = ProgressBar.ProgressBarStyle(background, knob)
        lua.getFieldString(3, "knobBefore")?.let {
            val drawable = luaApi.api.skinResolvers.resolveDrawable(theme, it)
            progressBarStyle.knobBefore = drawable
        }
        lua.getFieldString(3, "knobAfter")?.let {
            val drawable = luaApi.api.skinResolvers.resolveDrawable(theme, it)
            progressBarStyle.knobAfter = drawable
        }
        lua.getFieldString(3, "disabledBackground")?.let {
            val drawable = luaApi.api.skinResolvers.resolveDrawable(theme, it)
            progressBarStyle.disabledBackground = drawable
        }
        lua.getFieldString(3, "disabledKnob")?.let {
            val drawable = luaApi.api.skinResolvers.resolveDrawable(theme, it)
            progressBarStyle.disabledKnob = drawable
        }
        lua.getFieldString(3, "disabledKnobBefore")?.let {
            val drawable = luaApi.api.skinResolvers.resolveDrawable(theme, it)
            progressBarStyle.disabledKnobBefore = drawable
        }
        lua.getFieldString(3, "disabledKnobAfter")?.let {
            val drawable = luaApi.api.skinResolvers.resolveDrawable(theme, it)
            progressBarStyle.disabledKnobAfter = drawable
        }
        theme.skin.add(styleName, progressBarStyle)
        return 0
    }
}

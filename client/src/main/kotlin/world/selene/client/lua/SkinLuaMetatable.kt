package world.selene.client.lua

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.kotcrab.vis.ui.widget.VisTextField
import party.iroiro.luajava.Lua
import world.selene.client.assets.BundleFileResolver
import world.selene.common.lua.*

@Suppress("SameReturnValue")
class SkinLuaMetatable(
    private val bundleFileResolver: BundleFileResolver,
    private val luaSkinUtils: LuaSkinUtils
) {
    val luaMeta = LuaMappedMetatable(Skin::class) {
        callable(::luaAddTexture)
        callable(::luaAddButtonStyle)
        callable(::luaAddLabelStyle)
        callable(::luaAddImageButtonStyle)
        callable(::luaAddTextFieldStyle)
        callable(::luaAddScrollPaneStyle)
        callable(::luaAddProgressBarStyle)
    }

    /**
     * Adds a texture to the skin by name and path or LuaTexture object.
     * Throws error if texture file is not found.
     *
     * ```signatures
     * AddTexture(name: string, texturePath: string)
     * AddTexture(name: string, texture: LuaTexture)
     * ```
     */
    private fun luaAddTexture(lua: Lua): Int {
        val skin = lua.checkUserdata<Skin>(1)
        val name = lua.checkString(2)
        if (lua.isString(3)) {
            val texturePath = lua.checkString(3)
            val textureFile = bundleFileResolver.resolve(texturePath)
            if (!textureFile.exists()) {
                return lua.error(IllegalArgumentException("Texture file not found: $texturePath"))
            }

            val texture = Texture(textureFile)
            val region = TextureRegion(texture)
            skin.add(name, region)
        } else if (lua.isUserdata(3)) {
            val texture = lua.checkUserdata<LuaTexture>(3).texture
            val region = TextureRegion(texture)
            skin.add(name, region)
        }
        return 0
    }

    /**
     * Adds a button style to the skin.
     * Style properties are drawable paths that will be resolved using the skin.
     *
     * ```signatures
     * AddButtonStyle(name: string, config: table{up: string, down: string, checked: string, over: string})
     * ```
     */
    private fun luaAddButtonStyle(lua: Lua): Int {
        val skin = lua.checkUserdata<Skin>(1)
        val styleName = lua.checkString(2)
        lua.checkType(3, Lua.LuaType.TABLE)

        val up = lua.getFieldString(3, "up")?.let {
            luaSkinUtils.resolveDrawable(skin, it)
        }
        val down = lua.getFieldString(3, "down")?.let {
            luaSkinUtils.resolveDrawable(skin, it)
        }
        val checked = lua.getFieldString(3, "checked")?.let {
            luaSkinUtils.resolveDrawable(skin, it)
        }
        val buttonStyle = Button.ButtonStyle(up, down, checked)
        lua.getFieldString(3, "over")?.let {
            buttonStyle.over = luaSkinUtils.resolveDrawable(skin, it)
        }
        // TODO many more fields here to support
        skin.add(styleName, buttonStyle)
        return 0
    }

    /**
     * Adds a label style to the skin.
     * Font colors can be hex strings or color names. Background is optional.
     *
     * ```signatures
     * AddLabelStyle(name: string, config: table{font: string, fontColor: string, background: string})
     * ```
     */
    private fun luaAddLabelStyle(lua: Lua): Int {
        val skin = lua.checkUserdata<Skin>(1)
        val styleName = lua.checkString(2)
        lua.checkType(3, Lua.LuaType.TABLE)

        val font = lua.getFieldString(3, "font")?.let {
            luaSkinUtils.resolveFont(skin, it)
        }
        val fontColor = lua.getFieldString(3, "fontColor")?.let {
            luaSkinUtils.resolveColor(skin, it)
        } ?: Color.WHITE
        val labelStyle = Label.LabelStyle(font, fontColor)
        lua.getFieldString(3, "background")?.let {
            labelStyle.background = luaSkinUtils.resolveDrawable(skin, it)
        }
        skin.add(styleName, labelStyle)
        return 0
    }

    /**
     * Adds an image button style to the skin.
     * Supports both button states (up, down, over) and image states (imageUp, imageDown, etc.).
     *
     * ```signatures
     * AddImageButtonStyle(name: string, config: table{up: string, down: string, over: string, imageUp: string, imageDown: string, imageOver: string})
     * ```
     */
    private fun luaAddImageButtonStyle(lua: Lua): Int {
        val skin = lua.checkUserdata<Skin>(1)
        val styleName = lua.checkString(2)
        val styles = luaSkinUtils.createImageButtonStyle(lua, 3, skin)
        for (style in styles) {
            skin.add(styleName, style)
        }
        return 0
    }

    /**
     * Adds a text field style to the skin.
     * Creates both regular TextField and VisTextField styles. Supports focused and disabled states.
     *
     * ```signatures
     * AddTextFieldStyle(name: string, config: table{font: string, fontColor: string, cursor: string, selection: string, background: string, focusedFontColor: string, disabledFontColor: string, focusedBackground: string, disabledBackground: string, messageFont: string, messageFontColor: string})
     * ```
     */
    private fun luaAddTextFieldStyle(lua: Lua): Int {
        val skin = lua.checkUserdata<Skin>(1)
        val styleName = lua.checkString(2)
        lua.checkType(3, Lua.LuaType.TABLE)

        val font = lua.getFieldString(3, "font")?.let {
            luaSkinUtils.resolveFont(skin, it)
        }
        val fontColor = lua.getFieldString(3, "fontColor")?.let {
            luaSkinUtils.resolveColor(skin, it)
        } ?: Color.WHITE
        val cursor = lua.getFieldString(3, "cursor")?.let {
            luaSkinUtils.resolveDrawable(skin, it)
        }
        val selection = lua.getFieldString(3, "selection")?.let {
            luaSkinUtils.resolveDrawable(skin, it)
        }
        val background = lua.getFieldString(3, "background")?.let {
            luaSkinUtils.resolveDrawable(skin, it)
        }
        val textFieldStyle = TextField.TextFieldStyle(font, fontColor, cursor, selection, background)
        val visTextFieldStyle = VisTextField.VisTextFieldStyle(font, fontColor, cursor, selection, background)

        lua.getFieldString(3, "focusedFontColor")?.let {
            val color = luaSkinUtils.resolveColor(skin, it)
            textFieldStyle.focusedFontColor = color
            visTextFieldStyle.focusedFontColor = color
        }

        lua.getFieldString(3, "disabledFontColor")?.let {
            val color = luaSkinUtils.resolveColor(skin, it)
            textFieldStyle.disabledFontColor = color
            visTextFieldStyle.disabledFontColor = color
        }

        lua.getFieldString(3, "focusedBackground")?.let {
            val drawable = luaSkinUtils.resolveDrawable(skin, it)
            textFieldStyle.focusedBackground = drawable
            visTextFieldStyle.focusedBackground = drawable
        }

        lua.getFieldString(3, "disabledBackground")?.let {
            val drawable = luaSkinUtils.resolveDrawable(skin, it)
            textFieldStyle.disabledBackground = drawable
            visTextFieldStyle.disabledBackground = drawable
        }

        lua.getFieldString(3, "messageFont")?.let {
            val font = luaSkinUtils.resolveFont(skin, it)
            textFieldStyle.messageFont = font
            visTextFieldStyle.messageFont = font
        }

        lua.getFieldString(3, "messageFontColor")?.let {
            val color = luaSkinUtils.resolveColor(skin, it)
            textFieldStyle.messageFontColor = color
            visTextFieldStyle.messageFontColor = color
        }

        skin.add(styleName, textFieldStyle)
        return 0
    }

    /**
     * Adds a scroll pane style to the skin.
     * All drawable properties are optional and will be resolved using the skin.
     *
     * ```signatures
     * AddScrollPaneStyle(name: string, config: table{background: string, hScroll: string, hScrollKnob: string, vScroll: string, vScrollKnob: string, corner: string})
     * ```
     */
    private fun luaAddScrollPaneStyle(lua: Lua): Int {
        val skin = lua.checkUserdata<Skin>(1)
        val styleName = lua.checkString(2)
        lua.checkType(3, Lua.LuaType.TABLE)

        val background = lua.getFieldString(3, "background")?.let {
            luaSkinUtils.resolveDrawable(skin, it)
        }
        val hScroll = lua.getFieldString(3, "hScroll")?.let {
            luaSkinUtils.resolveDrawable(skin, it)
        }
        val hScrollKnob = lua.getFieldString(3, "hScrollKnob")?.let {
            luaSkinUtils.resolveDrawable(skin, it)
        }
        val vScroll = lua.getFieldString(3, "vScroll")?.let {
            luaSkinUtils.resolveDrawable(skin, it)
        }
        val vScrollKnob = lua.getFieldString(3, "vScrollKnob")?.let {
            luaSkinUtils.resolveDrawable(skin, it)
        }
        val scrollPaneStyle = ScrollPane.ScrollPaneStyle(background, hScroll, hScrollKnob, vScroll, vScrollKnob)
        lua.getFieldString(3, "corner")?.let {
            val drawable = luaSkinUtils.resolveDrawable(skin, it)
            scrollPaneStyle.corner = drawable
        }
        skin.add(styleName, scrollPaneStyle)
        return 0
    }

    /**
     * Adds a progress bar style to the skin.
     * Supports both normal and disabled states for all drawable properties.
     *
     * ```signatures
     * AddProgressBarStyle(name: string, config: table{background: string, knob: string, knobBefore: string, knobAfter: string, disabledBackground: string, disabledKnob: string, disabledKnobBefore: string, disabledKnobAfter: string})
     * ```
     */
    private fun luaAddProgressBarStyle(lua: Lua): Int {
        val skin = lua.checkUserdata<Skin>(1)
        val styleName = lua.checkString(2)
        lua.checkType(3, Lua.LuaType.TABLE)

        val background = lua.getFieldString(3, "background")?.let {
            luaSkinUtils.resolveDrawable(skin, it)
        }
        val knob = lua.getFieldString(3, "knob")?.let {
            luaSkinUtils.resolveDrawable(skin, it)
        }
        val progressBarStyle = ProgressBar.ProgressBarStyle(background, knob)
        lua.getFieldString(3, "knobBefore")?.let {
            val drawable = luaSkinUtils.resolveDrawable(skin, it)
            progressBarStyle.knobBefore = drawable
        }
        lua.getFieldString(3, "knobAfter")?.let {
            val drawable = luaSkinUtils.resolveDrawable(skin, it)
            progressBarStyle.knobAfter = drawable
        }
        lua.getFieldString(3, "disabledBackground")?.let {
            val drawable = luaSkinUtils.resolveDrawable(skin, it)
            progressBarStyle.disabledBackground = drawable
        }
        lua.getFieldString(3, "disabledKnob")?.let {
            val drawable = luaSkinUtils.resolveDrawable(skin, it)
            progressBarStyle.disabledKnob = drawable
        }
        lua.getFieldString(3, "disabledKnobBefore")?.let {
            val drawable = luaSkinUtils.resolveDrawable(skin, it)
            progressBarStyle.disabledKnobBefore = drawable
        }
        lua.getFieldString(3, "disabledKnobAfter")?.let {
            val drawable = luaSkinUtils.resolveDrawable(skin, it)
            progressBarStyle.disabledKnobAfter = drawable
        }
        skin.add(styleName, progressBarStyle)
        return 0
    }
}
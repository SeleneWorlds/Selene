package world.selene.client.ui.lua

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.kotcrab.vis.ui.widget.VisImageButton
import party.iroiro.luajava.Lua
import world.selene.client.bundles.BundleFileResolver
import world.selene.client.rendering.visual2d.Visual2D
import world.selene.client.ui.drawable.DrawableDrawable
import world.selene.client.ui.drawable.Visual2DDrawable
import world.selene.common.lua.util.checkType
import world.selene.common.lua.util.getField
import world.selene.common.lua.util.getFieldFloat

class LuaSkinUtils(private val bundleFileResolver: BundleFileResolver) {

    fun resolveFont(skin: Skin, fontName: String): BitmapFont {
        skin.optional(fontName, BitmapFont::class.java)?.let { return it }

        val fontFile = bundleFileResolver.resolve(fontName)
        if (fontFile.exists()) {
            return BitmapFont(fontFile)
        } else {
            throw IllegalArgumentException("Font not found in skin or file system: $fontName")
        }
    }

    fun resolveDrawable(skin: Skin?, path: String): Drawable? {
        skin?.optional(path, TextureRegion::class.java)?.let {
            return TextureRegionDrawable(it)
        }

        val textureFile = bundleFileResolver.resolve(path)
        return if (textureFile.exists()) {
            TextureRegionDrawable(TextureRegion(Texture(textureFile)))
        } else {
            null
        }
    }

    fun resolveColor(skin: Skin, colorString: String): Color {
        return when {
            colorString.startsWith("#") -> {
                try {
                    val hex = colorString.substring(1)
                    when (hex.length) {
                        6 -> Color.valueOf(hex + "FF")
                        8 -> Color.valueOf(hex)
                        else -> throw IllegalArgumentException("Invalid hex color: $colorString")
                    }
                } catch (e: Exception) {
                    throw IllegalArgumentException("Invalid hex color: $colorString", e)
                }
            }

            else -> {
                skin.optional(colorString, Color::class.java)
                    ?: throw IllegalArgumentException("Color not found in skin: $colorString")
            }
        }
    }

    fun createButtonStyle(
        lua: Lua,
        tableIndex: Int,
        skin: Skin? = null
    ): List<Button.ButtonStyle> {
        lua.checkType(tableIndex, Lua.LuaType.TABLE)

        val up = lua.getFieldDrawable(tableIndex, "up", skin)
        val down = lua.getFieldDrawable(tableIndex, "down", skin)
        val checked = lua.getFieldDrawable(tableIndex, "checked", skin)
        val buttonStyle = Button.ButtonStyle(up, down, checked)
        lua.getFieldDrawable(tableIndex, "over", skin)?.let {
            buttonStyle.over = it
        }
        lua.getFieldDrawable(tableIndex, "focused", skin)?.let {
            buttonStyle.focused = it
        }
        lua.getFieldDrawable(tableIndex, "disabled", skin)?.let {
            buttonStyle.disabled = it
        }
        lua.getFieldDrawable(tableIndex, "checkedOver", skin)?.let {
            buttonStyle.checkedOver = it
        }
        lua.getFieldDrawable(tableIndex, "checkedDown", skin)?.let {
            buttonStyle.checkedDown = it
        }
        lua.getFieldDrawable(tableIndex, "checkedFocused", skin)?.let {
            buttonStyle.checkedFocused = it
        }
        lua.getFieldFloat(tableIndex, "checkedOffsetX")?.let {
            buttonStyle.checkedOffsetX = it
        }
        lua.getFieldFloat(tableIndex, "checkedOffsetY")?.let {
            buttonStyle.checkedOffsetY = it
        }
        lua.getFieldFloat(tableIndex, "pressedOffsetX")?.let {
            buttonStyle.pressedOffsetX = it
        }
        lua.getFieldFloat(tableIndex, "pressedOffsetY")?.let {
            buttonStyle.pressedOffsetY = it
        }
        lua.getFieldFloat(tableIndex, "unpressedOffsetX")?.let {
            buttonStyle.unpressedOffsetX = it
        }
        lua.getFieldFloat(tableIndex, "unpressedOffsetY")?.let {
            buttonStyle.unpressedOffsetY = it
        }
        return listOf(buttonStyle)
    }

    fun createImageButtonStyle(
        lua: Lua,
        tableIndex: Int,
        skin: Skin? = null
    ): List<Button.ButtonStyle> {
        lua.checkType(tableIndex, Lua.LuaType.TABLE)

        val up = lua.getFieldDrawable(tableIndex, "up", skin)
        val down = lua.getFieldDrawable(tableIndex, "down", skin)
        val checked = lua.getFieldDrawable(tableIndex, "checked", skin)
        val imageUp = lua.getFieldDrawable(tableIndex, "imageUp", skin)
        val imageDown = lua.getFieldDrawable(tableIndex, "imageDown", skin)
        val imageChecked = lua.getFieldDrawable(tableIndex, "imageChecked", skin)
        val imageButtonStyle = ImageButton.ImageButtonStyle(up, down, checked, imageUp, imageDown, imageChecked)
        val visImageButtonStyle = VisImageButton.VisImageButtonStyle(
            up, down, checked, imageUp, imageDown, imageChecked
        )

        lua.getFieldDrawable(tableIndex, "over", skin)?.let {
            imageButtonStyle.over = it
            visImageButtonStyle.over = it
        }

        lua.getFieldDrawable(tableIndex, "checkedOver", skin)?.let {
            imageButtonStyle.checkedOver = it
            visImageButtonStyle.checkedOver = it
        }

        lua.getFieldDrawable(tableIndex, "disabled", skin)?.let {
            imageButtonStyle.disabled = it
            visImageButtonStyle.disabled = it
        }

        lua.getFieldDrawable(tableIndex, "imageOver", skin)?.let {
            imageButtonStyle.imageOver = it
            visImageButtonStyle.imageOver = it
        }

        lua.getFieldDrawable(tableIndex, "imageCheckedOver", skin)?.let {
            imageButtonStyle.imageCheckedOver = it
            visImageButtonStyle.imageCheckedOver = it
        }

        lua.getFieldDrawable(tableIndex, "imageDisabled", skin)?.let {
            imageButtonStyle.imageDisabled = it
            visImageButtonStyle.imageDisabled = it
        }

        return listOf(imageButtonStyle, visImageButtonStyle)
    }

    fun Lua.getFieldDrawable(tableIndex: Int, fieldName: String, skin: Skin?): Drawable? {
        return getField(tableIndex, fieldName) { type ->
            when (type) {
                Lua.LuaType.STRING -> resolveDrawable(skin, toString(-1)!!)
                Lua.LuaType.USERDATA -> {
                    when (val value = toJavaObject(-1)) {
                        is Visual2D -> Visual2DDrawable(value)
                        is world.selene.client.rendering.drawable.Drawable -> DrawableDrawable(value)
                        else -> null
                    }
                }

                else -> null
            }
        }
    }
}
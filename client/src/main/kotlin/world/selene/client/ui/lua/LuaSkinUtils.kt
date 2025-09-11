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
import world.selene.common.lua.util.getFieldString

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

        val up = lua.getFieldString(tableIndex, "up")?.let {
            resolveDrawable(skin, it)
        }
        val down = lua.getFieldString(tableIndex, "down")?.let {
            resolveDrawable(skin, it)
        }
        val checked = lua.getFieldString(tableIndex, "checked")?.let {
            resolveDrawable(skin, it)
        }
        val buttonStyle = Button.ButtonStyle(up, down, checked)
        lua.getFieldString(tableIndex, "over")?.let {
            buttonStyle.over = resolveDrawable(skin, it)
        }
        lua.getFieldString(tableIndex, "focused")?.let {
            buttonStyle.focused = resolveDrawable(skin, it)
        }
        lua.getFieldString(tableIndex, "disabled")?.let {
            buttonStyle.disabled = resolveDrawable(skin, it)
        }
        lua.getFieldString(tableIndex, "checkedOver")?.let {
            buttonStyle.checkedOver = resolveDrawable(skin, it)
        }
        lua.getFieldString(tableIndex, "checkedDown")?.let {
            buttonStyle.checkedDown = resolveDrawable(skin, it)
        }
        lua.getFieldString(tableIndex, "checkedFocused")?.let {
            buttonStyle.checkedFocused = resolveDrawable(skin, it)
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

        val up = lua.getFieldString(tableIndex, "up")?.let {
            resolveDrawable(skin, it)
        }
        val down = lua.getFieldString(tableIndex, "down")?.let {
            resolveDrawable(skin, it)
        }
        val checked = lua.getFieldString(tableIndex, "checked")?.let {
            resolveDrawable(skin, it)
        }
        val imageUp = lua.getField(tableIndex, "imageUp") { type ->
            when (type) {
                Lua.LuaType.STRING -> resolveDrawable(skin, lua.toString(-1)!!)
                Lua.LuaType.USERDATA -> {
                    when (val value = lua.toJavaObject(-1)) {
                        is Visual2D -> Visual2DDrawable(value)
                        is world.selene.client.rendering.drawable.Drawable -> DrawableDrawable(value)
                        else -> null
                    }
                }

                else -> null
            }
        }
        val imageDown = lua.getFieldString(tableIndex, "imageDown")?.let {
            resolveDrawable(skin, it)
        }
        val imageChecked = lua.getFieldString(tableIndex, "imageChecked")?.let {
            resolveDrawable(skin, it)
        }
        val imageButtonStyle = ImageButton.ImageButtonStyle(up, down, checked, imageUp, imageDown, imageChecked)
        val visImageButtonStyle =
            VisImageButton.VisImageButtonStyle(up, down, checked, imageUp, imageDown, imageChecked)

        lua.getFieldString(tableIndex, "over")?.let {
            val drawable = resolveDrawable(skin, it)
            imageButtonStyle.over = drawable
            visImageButtonStyle.over = drawable
        }

        lua.getFieldString(tableIndex, "checkedOver")?.let {
            val drawable = resolveDrawable(skin, it)
            imageButtonStyle.checkedOver = drawable
            visImageButtonStyle.checkedOver = drawable
        }

        lua.getFieldString(tableIndex, "disabled")?.let {
            val drawable = resolveDrawable(skin, it)
            imageButtonStyle.disabled = drawable
            visImageButtonStyle.disabled = drawable
        }

        lua.getFieldString(tableIndex, "imageOver")?.let {
            val drawable = resolveDrawable(skin, it)
            imageButtonStyle.imageOver = drawable
            visImageButtonStyle.imageOver = drawable
        }

        lua.getFieldString(tableIndex, "imageCheckedOver")?.let {
            val drawable = resolveDrawable(skin, it)
            imageButtonStyle.imageCheckedOver = drawable
            visImageButtonStyle.imageCheckedOver = drawable
        }

        lua.getFieldString(tableIndex, "imageDisabled")?.let {
            val drawable = resolveDrawable(skin, it)
            imageButtonStyle.imageDisabled = drawable
            visImageButtonStyle.imageDisabled = drawable
        }

        return listOf(imageButtonStyle, visImageButtonStyle)
    }
}
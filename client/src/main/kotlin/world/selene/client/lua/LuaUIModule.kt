package world.selene.client.lua

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.I18NBundle
import com.github.czyzby.lml.parser.impl.tag.actor.provider.LabelLmlTagProvider
import com.github.czyzby.lml.vis.util.VisLml
import com.kotcrab.vis.ui.widget.BusyBar
import com.kotcrab.vis.ui.widget.LinkLabel
import com.kotcrab.vis.ui.widget.Separator
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisImageButton.VisImageButtonStyle
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisProgressBar
import com.kotcrab.vis.ui.widget.VisTextField
import com.kotcrab.vis.ui.widget.color.internal.Palette
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.assets.BundleFileResolver
import world.selene.client.ui.UI
import world.selene.common.lua.*


class LuaUIModule(private val ui: UI, private val bundleFileResolver: BundleFileResolver) : LuaModule {
    override val name = "selene.ui.lml"

    override fun initialize(luaManager: LuaManager) {
        luaManager.exposeClass(ActorLuaProxy::class)
        luaManager.exposeClass(SkinLuaProxy::class)
    }

    override fun register(table: LuaValue) {
        table.register("LoadUI", this::luaLoadUI)
        table.register("LoadSkin", this::luaLoadSkin)
        table.register("CreateSkin", this::luaCreateSkin)
        table.register("AddToRoot", this::luaAddToRoot)
        table.set("Root", ActorLuaProxy(ui.bundlesRoot))
    }

    private fun luaAddToRoot(lua: Lua): Int {
        if (lua.isTable(1)) {
            val actors = lua.toMap(1)?.values ?: emptyList()
            for (actor in actors) {
                if (actor is ActorLuaProxy) {
                    ui.bundlesRoot.add(actor.delegate)
                }
            }
        } else if (lua.isUserdata(1)) {
            val actor = lua.checkJavaObject(1, ActorLuaProxy::class).delegate
            ui.bundlesRoot.add(actor)
        }
        return 0
    }

    private fun luaLoadUI(lua: Lua): Int {
        val xmlFilePath = lua.checkString(1)

        val actions = mutableMapOf<String, LuaValue>()
        var i18nBundle = "system"
        var skin: Skin? = null

        // Load options from second parameter
        if (lua.isTable(2)) {
            lua.getField(2, "actions")
            if (lua.isTable(-1)) {
                lua.toMap(-1)?.entries?.forEach { (actionName, actionFunction) ->
                    if (actionName is String && actionFunction is LuaValue) {
                        actions[actionName] = actionFunction
                    }
                }
            }
            lua.pop(1)

            lua.getField(2, "i18nBundle")
            if (lua.isString(-1)) {
                i18nBundle = lua.toString(-1) ?: "system"
            }
            lua.pop(1)

            lua.getField(2, "skin")
            if (lua.isUserdata(-1)) {
                skin = lua.checkJavaObject(-1, SkinLuaProxy::class)?.delegate
            }
            lua.pop(1)
        }

        try {
            val parser = VisLml.parser().skin(skin ?: ui.systemSkin)

            // VisLabel forces the VisUI skin and provides no other benefits. It makes no sense to be applied to label.
            // Therefore, we revert "label" to use the default provider instead of the visui-lml one.
            parser.tag(LabelLmlTagProvider(), "label")

            // Register actions from Lua
            for ((actionName, actionFunction) in actions) {
                parser.action(actionName) { actor ->
                    try {
                        actionFunction.call(ActorLuaProxy(actor as Actor))
                    } catch (e: Exception) {
                        // Log error but don't crash the UI
                        println("Error executing Lua action '$actionName': ${e.message}")
                    }
                }
            }

            // Set up i18n bundle
            val i18nFileHandle = bundleFileResolver.resolve(i18nBundle)
            val i18nBundle = I18NBundle.createBundle(i18nFileHandle)
            parser.i18nBundle(i18nBundle)

            // Parse the UI XML file
            val xmlFile = bundleFileResolver.resolve(xmlFilePath)
            if (!xmlFile.exists()) {
                return lua.error(IllegalArgumentException("XML file not found: $xmlFilePath"))
            }

            val actors = parser.build().parseTemplate(xmlFile)

            val actorsByName = mutableMapOf<String, ActorLuaProxy>()
            fun collectActorsByName(actor: Actor) {
                if (actor.name != null && actor.name.isNotEmpty()) {
                    actorsByName[actor.name] = ActorLuaProxy(actor)
                }
                if (actor is Group) {
                    actor.children.forEach { child ->
                        collectActorsByName(child)
                    }
                }
            }
            actors.forEach { collectActorsByName(it) }

            lua.push(actors.map { ActorLuaProxy(it) }, Lua.Conversion.FULL)
            lua.push(actorsByName, Lua.Conversion.FULL)
            return 2
        } catch (e: Exception) {
            return lua.error(e)
        }
    }

    private fun luaLoadSkin(lua: Lua): Int {
        val skinPath = lua.checkString(1)

        try {
            val skinFile = bundleFileResolver.resolve(skinPath)
            if (!skinFile.exists()) {
                return lua.error(IllegalArgumentException("Skin file not found: $skinPath"))
            }

            val skin = Skin(skinFile)
            lua.push(SkinLuaProxy(skin, bundleFileResolver), Lua.Conversion.NONE)
            return 1
        } catch (e: Exception) {
            return lua.error(e)
        }
    }

    private fun luaCreateSkin(lua: Lua): Int {
        val font = BitmapFont()
        val skin = Skin().apply {
            add("default", font)
            add("default", Label.LabelStyle(font, Color.WHITE))
        }
        lua.push(SkinLuaProxy(skin, bundleFileResolver), Lua.Conversion.NONE)
        return 1
    }

    class ActorLuaProxy(val delegate: Actor) {
        fun AddChild(child: ActorLuaProxy) {
            when (delegate) {
                is Container<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    (delegate as Container<Actor>).actor = child.delegate
                }

                is Group -> {
                    delegate.addActor(child.delegate)
                }

                else -> {
                    throw IllegalArgumentException("Actor of type ${delegate.javaClass.simpleName} cannot have children")
                }
            }
        }

        fun SetStyle(lua: Lua): Int {
            val skin = lua.checkJavaObject(2, SkinLuaProxy::class).delegate
            val style = lua.checkString(3)
            when (delegate) {
                is VisImageButton -> {
                    delegate.style = skin.get(style, VisImageButtonStyle::class.java)
                }

                is VisTextField -> {
                    delegate.style = skin.get(style, VisTextField.VisTextFieldStyle::class.java)
                }

                is ImageButton -> {
                    delegate.style = skin.get(style, ImageButton.ImageButtonStyle::class.java)
                }

                is LinkLabel -> {
                    delegate.style = skin.get(style, LinkLabel.LinkLabelStyle::class.java)
                }

                is Label -> {
                    delegate.style = skin.get(style, Label.LabelStyle::class.java)
                }

                is SelectBox<*> -> {
                    delegate.style = skin.get(style, SelectBox.SelectBoxStyle::class.java)
                }

                is Slider -> {
                    delegate.style = skin.get(style, Slider.SliderStyle::class.java)
                }

                is ProgressBar -> {
                    delegate.style = skin.get(style, ProgressBar.ProgressBarStyle::class.java)
                }

                is TextField -> {
                    delegate.style = skin.get(style, TextField.TextFieldStyle::class.java)
                }

                is List<*> -> {
                    delegate.style = skin.get(style, List.ListStyle::class.java)
                }

                is Touchpad -> {
                    delegate.style = skin.get(style, Touchpad.TouchpadStyle::class.java)
                }
            }
            return 0
        }

        fun SetText(text: String) {
            when (delegate) {
                is Label -> {
                    delegate.setText(text)
                }

                is TextField -> {
                    delegate.setText(text)
                }
            }
        }

        fun GetText(): String {
            return when (delegate) {
                is Label -> {
                    delegate.text.toString()
                }

                is TextField -> {
                    delegate.text
                }

                else -> throw IllegalArgumentException("Widget of type ${delegate.javaClass.simpleName} does not have text")
            }
        }
    }

    class SkinLuaProxy(val delegate: Skin, private val bundleFileResolver: BundleFileResolver) {
        fun AddTexture(lua: Lua): Int {
            val name = lua.checkString(2)
            val texturePath = lua.checkString(3)

            try {
                val textureFile = bundleFileResolver.resolve(texturePath)
                if (!textureFile.exists()) {
                    return lua.error(IllegalArgumentException("Texture file not found: $texturePath"))
                }

                val texture = Texture(textureFile)
                val region = TextureRegion(texture)
                delegate.add(name, region)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add texture region '$name': ${e.message}", e))
            }
        }

        fun AddButtonStyle(lua: Lua): Int {
            val styleName = lua.checkString(2)

            try {
                val buttonStyle = Button.ButtonStyle()

                if (lua.isTable(2)) {
                    lua.getField(2, "up")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        buttonStyle.up = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(2, "down")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        buttonStyle.down = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(2, "over")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        buttonStyle.over = resolveDrawable(path)
                    }
                    lua.pop(1)
                }

                delegate.add(styleName, buttonStyle)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add button style '$styleName': ${e.message}", e))
            }
        }

        fun AddLabelStyle(lua: Lua): Int {
            val styleName = lua.checkString(2)

            try {
                val labelStyle = Label.LabelStyle()

                if (lua.isTable(3)) {
                    lua.getField(3, "font")
                    if (lua.isString(-1)) {
                        val fontName = lua.toString(-1)!!
                        labelStyle.font = resolveFont(fontName)
                    }
                    lua.pop(1)

                    lua.getField(3, "fontColor")
                    if (lua.isString(-1)) {
                        val colorString = lua.toString(-1)!!
                        labelStyle.fontColor = resolveColor(colorString)
                    }
                    lua.pop(1)

                    lua.getField(3, "background")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        labelStyle.background = resolveDrawable(path)
                    }
                    lua.pop(1)
                }

                delegate.add(styleName, labelStyle)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add label style '$styleName': ${e.message}", e))
            }
        }

        fun AddImageButtonStyle(lua: Lua): Int {
            val styleName = lua.checkString(2)

            try {
                val imageButtonStyle = VisImageButtonStyle()

                if (lua.isTable(3)) {
                    lua.getField(3, "up")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        imageButtonStyle.up = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "down")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        imageButtonStyle.down = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "over")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        imageButtonStyle.over = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "checked")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        imageButtonStyle.checked = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "checkedOver")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        imageButtonStyle.checkedOver = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "disabled")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        imageButtonStyle.disabled = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "imageUp")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        imageButtonStyle.imageUp = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "imageDown")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        imageButtonStyle.imageDown = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "imageOver")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        imageButtonStyle.imageOver = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "imageChecked")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        imageButtonStyle.imageChecked = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "imageCheckedOver")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        imageButtonStyle.imageCheckedOver = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "imageDisabled")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        imageButtonStyle.imageDisabled = resolveDrawable(path)
                    }
                    lua.pop(1)
                }

                delegate.add(styleName, imageButtonStyle)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add image button style '$styleName': ${e.message}", e))
            }
        }

        fun AddTextFieldStyle(lua: Lua): Int {
            val styleName = lua.checkString(2)

            try {
                val textFieldStyle = TextField.TextFieldStyle().apply {
                    fontColor = Color.WHITE
                }

                if (lua.isTable(3)) {
                    lua.getField(3, "font")
                    if (lua.isString(-1)) {
                        val fontName = lua.toString(-1)!!
                        textFieldStyle.font = resolveFont(fontName)
                    }
                    lua.pop(1)

                    lua.getField(3, "fontColor")
                    if (lua.isString(-1)) {
                        val colorString = lua.toString(-1)!!
                        textFieldStyle.fontColor = resolveColor(colorString)
                    }
                    lua.pop(1)

                    lua.getField(3, "focusedFontColor")
                    if (lua.isString(-1)) {
                        val colorString = lua.toString(-1)!!
                        textFieldStyle.focusedFontColor = resolveColor(colorString)
                    }
                    lua.pop(1)

                    lua.getField(3, "disabledFontColor")
                    if (lua.isString(-1)) {
                        val colorString = lua.toString(-1)!!
                        textFieldStyle.disabledFontColor = resolveColor(colorString)
                    }
                    lua.pop(1)

                    lua.getField(3, "background")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        textFieldStyle.background = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "focusedBackground")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        textFieldStyle.focusedBackground = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "disabledBackground")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        textFieldStyle.disabledBackground = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "cursor")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        textFieldStyle.cursor = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "selection")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        textFieldStyle.selection = resolveDrawable(path)
                    }
                    lua.pop(1)

                    lua.getField(3, "messageFont")
                    if (lua.isString(-1)) {
                        val fontName = lua.toString(-1)!!
                        textFieldStyle.messageFont = resolveFont(fontName)
                    }
                    lua.pop(1)

                    lua.getField(3, "messageFontColor")
                    if (lua.isString(-1)) {
                        val colorString = lua.toString(-1)!!
                        textFieldStyle.messageFontColor = resolveColor(colorString)
                    }
                    lua.pop(1)
                }

                delegate.add(styleName, textFieldStyle)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add text field style '$styleName': ${e.message}", e))
            }
        }

        private fun resolveFont(fontName: String): BitmapFont {
            delegate.optional(fontName, BitmapFont::class.java)?.let { return it }

            val fontFile = bundleFileResolver.resolve(fontName)
            if (fontFile.exists()) {
                return BitmapFont(fontFile)
            } else {
                throw IllegalArgumentException("Font not found in skin or file system: $fontName")
            }
        }

        private fun resolveDrawable(path: String): Drawable? {
            delegate.optional(path, TextureRegion::class.java)?.let {
                return TextureRegionDrawable(it)
            }

            val textureFile = bundleFileResolver.resolve(path)
            return if (textureFile.exists()) {
                TextureRegionDrawable(TextureRegion(Texture(textureFile)))
            } else {
                null
            }
        }

        private fun resolveColor(colorString: String): Color {
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
                    delegate.optional(colorString, Color::class.java)
                        ?: throw IllegalArgumentException("Color not found in skin: $colorString")
                }
            }
        }
    }
}
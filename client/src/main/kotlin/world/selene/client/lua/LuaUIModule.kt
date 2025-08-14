package world.selene.client.lua

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.I18NBundle
import com.kotcrab.vis.ui.widget.LinkLabel
import com.kotcrab.vis.ui.widget.VisImageButton
import com.kotcrab.vis.ui.widget.VisImageButton.VisImageButtonStyle
import com.kotcrab.vis.ui.widget.VisTextField
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.assets.BundleFileResolver
import world.selene.client.ui.SeleneLmlParser
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
        table.register("CreateContainer", this::luaCreateContainer)
        table.register("CreateLabel", this::luaCreateLabel)
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
                skin = lua.checkJavaObject(-1, SkinLuaProxy::class).delegate
            }
            lua.pop(1)
        }

        try {
            val parser = SeleneLmlParser.parser().skin(skin ?: ui.systemSkin)

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
            lua.push(SkinLuaProxy(skin, this, bundleFileResolver), Lua.Conversion.NONE)
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
        lua.push(SkinLuaProxy(skin, this, bundleFileResolver), Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateContainer(lua: Lua): Int {
        val skin = lua.checkJavaObject(1, SkinLuaProxy::class).delegate

        val container = Container<Actor>()

        if (lua.top > 1) {
            lua.checkType(2, Lua.LuaType.TABLE)

            lua.getField(2, "background")
            if (!lua.isNil(-1)) {
                container.background = resolveDrawable(skin, lua.toString(-1)!!)
            }
            lua.pop(1)

            lua.getField(2, "child")
            if (!lua.isNil(-1)) {
                container.actor = lua.checkJavaObject(-1, ActorLuaProxy::class).delegate
            }
            lua.pop(1)

            lua.getField(2, "width")
            if (!lua.isNil(-1)) {
                container.width(lua.checkFloat(-1))
            }
            lua.pop(1)

            lua.getField(2, "height")
            if (!lua.isNil(-1)) {
                container.height(lua.checkFloat(-1))
            }
            lua.pop(1)
        }

        lua.push(ActorLuaProxy(container), Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateLabel(lua: Lua): Int {
        val skin = lua.checkJavaObject(1, SkinLuaProxy::class).delegate

        var text = ""
        var styleName = "default"
        var wrap = false

        if (lua.top > 1) {
            lua.checkType(2, Lua.LuaType.TABLE)

            lua.getField(2, "text")
            if (!lua.isNil(-1)) {
                text = lua.checkString(-1)
            }
            lua.pop(1)

            lua.getField(2, "style")
            if (!lua.isNil(-1)) {
                styleName = lua.checkString(-1)
            }
            lua.pop(1)

            lua.getField(2, "wrap")
            if (!lua.isNil(-1)) {
                wrap = lua.checkBoolean(-1)
            }
            lua.pop(1)
        }

        try {
            val labelStyle = skin.get(styleName, Label.LabelStyle::class.java)
            val label = Label(text, labelStyle)
            label.wrap = wrap
            lua.push(ActorLuaProxy(label), Lua.Conversion.NONE)
            return 1
        } catch (e: Exception) {
            return lua.error(RuntimeException("Failed to create label: ${e.message}", e))
        }
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

        var Text: String?
            get() {
                return when (delegate) {
                    is Label -> {
                        delegate.text.toString()
                    }

                    is TextField -> {
                        delegate.text
                    }

                    else -> null
                }
            }
            set(value) {
                when (delegate) {
                    is Label -> {
                        delegate.setText(value)
                    }

                    is TextField -> {
                        delegate.setText(value)
                    }

                    else -> {
                        throw IllegalArgumentException("Widget of type ${delegate.javaClass.simpleName} cannot have text")
                    }
                }
            }

        var MinWidth
            get() = (delegate as? Layout)?.minWidth ?: 0f
            set(value) {
                when (delegate) {
                    is Container<*> -> delegate.minWidth(value)
                    else -> throw IllegalArgumentException("Widget of type ${delegate.javaClass.simpleName} cannot have minWidth")
                }
            }

        var MinHeight
            get() = (delegate as? Layout)?.minHeight ?: 0f
            set(value) {
                when (delegate) {
                    is Container<*> -> delegate.minHeight(value)
                    else -> throw IllegalArgumentException("Widget of type ${delegate.javaClass.simpleName} cannot have minHeight")
                }
            }

        var Width
            get() = delegate.width;
            set(value) {
                delegate.width = value
            }

        var Height
            get() = delegate.height;
            set(value) {
                delegate.height = value
            }

        val Parent: ActorLuaProxy?
            get() {
                return when (delegate.parent) {
                    null -> null
                    else -> ActorLuaProxy(delegate.parent)
                }
            }
    }

    class SkinLuaProxy(
        val delegate: Skin,
        private val module: LuaUIModule,
        private val bundleFileResolver: BundleFileResolver
    ) {
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
                        buttonStyle.up = module.resolveDrawable(delegate, path)
                    }
                    lua.pop(1)

                    lua.getField(2, "down")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        buttonStyle.down = module.resolveDrawable(delegate, path)
                    }
                    lua.pop(1)

                    lua.getField(2, "over")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        buttonStyle.over = module.resolveDrawable(delegate, path)
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
                        labelStyle.font = module.resolveFont(delegate, fontName)
                    }
                    lua.pop(1)

                    lua.getField(3, "fontColor")
                    if (lua.isString(-1)) {
                        val colorString = lua.toString(-1)!!
                        labelStyle.fontColor = module.resolveColor(delegate, colorString)
                    }
                    lua.pop(1)

                    lua.getField(3, "background")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        labelStyle.background = module.resolveDrawable(delegate, path)
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
                val imageButtonStyle = ImageButton.ImageButtonStyle()
                val visImageButtonStyle = VisImageButtonStyle()

                if (lua.isTable(3)) {
                    lua.getField(3, "up")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        imageButtonStyle.up = drawable
                        visImageButtonStyle.up = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "down")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        imageButtonStyle.down = drawable
                        visImageButtonStyle.down = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "over")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        imageButtonStyle.over = drawable
                        visImageButtonStyle.over = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "checked")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        imageButtonStyle.checked = drawable
                        visImageButtonStyle.checked = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "checkedOver")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        imageButtonStyle.checkedOver = drawable
                        visImageButtonStyle.checkedOver = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "disabled")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        imageButtonStyle.disabled = drawable
                        visImageButtonStyle.disabled = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "imageUp")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        imageButtonStyle.imageUp = drawable
                        visImageButtonStyle.imageUp = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "imageDown")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        imageButtonStyle.imageDown = drawable
                        visImageButtonStyle.imageDown = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "imageOver")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        imageButtonStyle.imageOver = drawable
                        visImageButtonStyle.imageOver = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "imageChecked")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        imageButtonStyle.imageChecked = drawable
                        visImageButtonStyle.imageChecked = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "imageCheckedOver")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        imageButtonStyle.imageCheckedOver = drawable
                        visImageButtonStyle.imageCheckedOver = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "imageDisabled")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        imageButtonStyle.imageDisabled = drawable
                        visImageButtonStyle.imageDisabled = drawable
                    }
                    lua.pop(1)
                }

                delegate.add(styleName, imageButtonStyle)
                delegate.add(styleName, visImageButtonStyle)
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
                val visTextFieldStyle = VisTextField.VisTextFieldStyle().apply {
                    fontColor = Color.WHITE
                }

                if (lua.isTable(3)) {
                    lua.getField(3, "font")
                    if (lua.isString(-1)) {
                        val fontName = lua.toString(-1)!!
                        val font = module.resolveFont(delegate, fontName)
                        textFieldStyle.font = font
                        visTextFieldStyle.font = font
                    }
                    lua.pop(1)

                    lua.getField(3, "fontColor")
                    if (lua.isString(-1)) {
                        val colorString = lua.toString(-1)!!
                        val color = module.resolveColor(delegate, colorString)
                        textFieldStyle.fontColor = color
                        visTextFieldStyle.fontColor = color
                    }
                    lua.pop(1)

                    lua.getField(3, "focusedFontColor")
                    if (lua.isString(-1)) {
                        val colorString = lua.toString(-1)!!
                        val color = module.resolveColor(delegate, colorString)
                        textFieldStyle.focusedFontColor = color
                        visTextFieldStyle.focusedFontColor = color
                    }
                    lua.pop(1)

                    lua.getField(3, "disabledFontColor")
                    if (lua.isString(-1)) {
                        val colorString = lua.toString(-1)!!
                        val color = module.resolveColor(delegate, colorString)
                        textFieldStyle.disabledFontColor = color
                        visTextFieldStyle.disabledFontColor = color
                    }
                    lua.pop(1)

                    lua.getField(3, "background")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        textFieldStyle.background = drawable
                        visTextFieldStyle.background = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "focusedBackground")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        textFieldStyle.focusedBackground = drawable
                        visTextFieldStyle.focusedBackground = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "disabledBackground")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        textFieldStyle.disabledBackground = drawable
                        visTextFieldStyle.disabledBackground = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "cursor")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        textFieldStyle.cursor = drawable
                        visTextFieldStyle.cursor = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "selection")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        val drawable = module.resolveDrawable(delegate, path)
                        textFieldStyle.selection = drawable
                        visTextFieldStyle.selection = drawable
                    }
                    lua.pop(1)

                    lua.getField(3, "messageFont")
                    if (lua.isString(-1)) {
                        val fontName = lua.toString(-1)!!
                        val font = module.resolveFont(delegate, fontName)
                        textFieldStyle.messageFont = font
                        visTextFieldStyle.messageFont = font
                    }
                    lua.pop(1)

                    lua.getField(3, "messageFontColor")
                    if (lua.isString(-1)) {
                        val colorString = lua.toString(-1)!!
                        val color = module.resolveColor(delegate, colorString)
                        textFieldStyle.messageFontColor = color
                        visTextFieldStyle.messageFontColor = color
                    }
                    lua.pop(1)
                }

                delegate.add(styleName, textFieldStyle)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add text field style '$styleName': ${e.message}", e))
            }
        }

        fun AddScrollPaneStyle(lua: Lua): Int {
            val styleName = lua.checkString(2)

            try {
                val scrollPaneStyle = ScrollPane.ScrollPaneStyle()

                if (lua.isTable(3)) {
                    lua.getField(3, "background")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        scrollPaneStyle.background = module.resolveDrawable(delegate, path)
                    }
                    lua.pop(1)

                    lua.getField(3, "corner")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        scrollPaneStyle.corner = module.resolveDrawable(delegate, path)
                    }
                    lua.pop(1)

                    lua.getField(3, "hScroll")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        scrollPaneStyle.hScroll = module.resolveDrawable(delegate, path)
                    }
                    lua.pop(1)

                    lua.getField(3, "hScrollKnob")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        scrollPaneStyle.hScrollKnob = module.resolveDrawable(delegate, path)
                    }
                    lua.pop(1)

                    lua.getField(3, "vScroll")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        scrollPaneStyle.vScroll = module.resolveDrawable(delegate, path)
                    }
                    lua.pop(1)

                    lua.getField(3, "vScrollKnob")
                    if (lua.isString(-1)) {
                        val path = lua.toString(-1)!!
                        scrollPaneStyle.vScrollKnob = module.resolveDrawable(delegate, path)
                    }
                    lua.pop(1)
                }

                delegate.add(styleName, scrollPaneStyle)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add scroll pane style '$styleName': ${e.message}", e))
            }
        }
    }

    private fun resolveFont(skin: Skin, fontName: String): BitmapFont {
        skin.optional(fontName, BitmapFont::class.java)?.let { return it }

        val fontFile = bundleFileResolver.resolve(fontName)
        if (fontFile.exists()) {
            return BitmapFont(fontFile)
        } else {
            throw IllegalArgumentException("Font not found in skin or file system: $fontName")
        }
    }

    private fun resolveDrawable(skin: Skin, path: String): Drawable? {
        skin.optional(path, TextureRegion::class.java)?.let {
            return TextureRegionDrawable(it)
        }

        val textureFile = bundleFileResolver.resolve(path)
        return if (textureFile.exists()) {
            TextureRegionDrawable(TextureRegion(Texture(textureFile)))
        } else {
            null
        }
    }

    private fun resolveColor(skin: Skin, colorString: String): Color {
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
}
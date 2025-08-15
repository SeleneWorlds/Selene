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
        luaManager.exposeClass(ProgressBarLuaProxy::class)
        luaManager.exposeClass(SkinLuaProxy::class)
    }

    override fun register(table: LuaValue) {
        table.register("LoadUI", this::luaLoadUI)
        table.register("LoadSkin", this::luaLoadSkin)
        table.register("CreateSkin", this::luaCreateSkin)
        table.register("CreateContainer", this::luaCreateContainer)
        table.register("CreateLabel", this::luaCreateLabel)
        table.register("AddToRoot", this::luaAddToRoot)
        table.set("Root", ActorLuaProxy.createProxy(ui.bundlesRoot))
    }

    private fun luaAddToRoot(lua: Lua): Int {
        if (lua.isTable(1)) {
            val actors = lua.toMap(1)?.values ?: emptyList()
            for (actor in actors) {
                if (actor is ActorLuaProxy<*>) {
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
        if (lua.top >= 2) {
            lua.checkType(2, Lua.LuaType.TABLE)
        }

        val actions = mutableMapOf<String, LuaValue>()
        val i18nBundle = lua.getFieldString(2, "i18nBundle") ?: "system"
        val skin = lua.getFieldJavaObject(2, "skin", SkinLuaProxy::class)?.delegate

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
        }

        try {
            val parser = SeleneLmlParser.parser().skin(skin ?: ui.systemSkin)

            // Register actions from Lua
            for ((actionName, actionFunction) in actions) {
                parser.action(actionName) { actor ->
                    try {
                        actionFunction.call(ActorLuaProxy.createProxy(actor as Actor))
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

            val actorsByName = mutableMapOf<String, ActorLuaProxy<*>>()
            fun collectActorsByName(actor: Actor) {
                if (actor.name != null && actor.name.isNotEmpty()) {
                    actorsByName[actor.name] = ActorLuaProxy.createProxy(actor)
                }
                if (actor is Group) {
                    actor.children.forEach { child ->
                        collectActorsByName(child)
                    }
                }
            }
            actors.forEach { collectActorsByName(it) }

            lua.push(actors.map { ActorLuaProxy.createProxy(it) }, Lua.Conversion.FULL)
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
        if (lua.top > 1) {
            lua.checkType(2, Lua.LuaType.TABLE)
        }

        val child = lua.getFieldJavaObject(2, "child", ActorLuaProxy::class)?.delegate
        val container = Container<Actor>(child)
        lua.getFieldString(2, "width")?.let {
            container.background = resolveDrawable(skin, lua.toString(-1)!!)
        }
        lua.getFieldFloat(2, "width")?.let {
            container.width(it)
        }
        lua.getFieldFloat(2, "height")?.let {
            container.height(it)
        }
        lua.push(ActorLuaProxy.createProxy(container), Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateLabel(lua: Lua): Int {
        val skin = lua.checkJavaObject(1, SkinLuaProxy::class).delegate
        if (lua.top > 1) {
            lua.checkType(2, Lua.LuaType.TABLE)
        }

        try {
            val style = lua.getFieldString(2, "style") ?: "default"
            val labelStyle = skin.get(style, Label.LabelStyle::class.java)
            val text = lua.getFieldString(2, "text") ?: ""
            val label = Label(text, labelStyle)
            label.wrap = lua.getFieldBoolean(2, "wrap") ?: false
            lua.push(ActorLuaProxy.createProxy(label), Lua.Conversion.NONE)
            return 1
        } catch (e: Exception) {
            return lua.error(RuntimeException("Failed to create label: ${e.message}", e))
        }
    }

    class ProgressBarLuaProxy(delegate: ProgressBar) : ActorLuaProxy<ProgressBar>(delegate) {
        var Value: Float
            get() {
                return delegate.value
            }
            set(value) {
                delegate.value = value
            }
    }

    open class ActorLuaProxy<T : Actor>(val delegate: T) {
        companion object {
            fun createProxy(actor: Actor): ActorLuaProxy<*> {
                return when (actor) {
                    is ProgressBar -> return ProgressBarLuaProxy(actor)
                    else -> ActorLuaProxy(actor)
                }
            }
        }

        fun AddChild(child: ActorLuaProxy<*>) {
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

        val Parent: ActorLuaProxy<*>?
            get() {
                return when (delegate.parent) {
                    null -> null
                    else -> createProxy(delegate.parent)
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
            lua.checkType(3, Lua.LuaType.TABLE)

            try {
                val up = lua.getFieldString(3, "up")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val down = lua.getFieldString(3, "down")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val checked = lua.getFieldString(3, "checked")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val buttonStyle = Button.ButtonStyle(up, down, checked)
                lua.getFieldString(3, "over")?.let {
                    buttonStyle.over = module.resolveDrawable(delegate, it)
                }
                // TODO many more fields here to support
                delegate.add(styleName, buttonStyle)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add button style '$styleName': ${e.message}", e))
            }
        }

        fun AddLabelStyle(lua: Lua): Int {
            val styleName = lua.checkString(2)
            lua.checkType(3, Lua.LuaType.TABLE)

            try {
                val font = lua.getFieldString(3, "font")?.let {
                    module.resolveFont(delegate, it)
                }
                val fontColor = lua.getFieldString(3, "fontColor")?.let {
                    module.resolveColor(delegate, it)
                } ?: Color.WHITE
                val labelStyle = Label.LabelStyle(font, fontColor)
                lua.getFieldString(3, "background")?.let {
                    labelStyle.background = module.resolveDrawable(delegate, it)
                }
                delegate.add(styleName, labelStyle)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add label style '$styleName': ${e.message}", e))
            }
        }

        fun AddImageButtonStyle(lua: Lua): Int {
            val styleName = lua.checkString(2)
            lua.checkType(3, Lua.LuaType.TABLE)

            try {
                val up = lua.getFieldString(3, "up")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val down = lua.getFieldString(3, "down")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val checked = lua.getFieldString(3, "checked")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val imageUp = lua.getFieldString(3, "imageUp")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val imageDown = lua.getFieldString(3, "imageDown")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val imageChecked = lua.getFieldString(3, "imageChecked")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val imageButtonStyle = ImageButton.ImageButtonStyle(up, down, checked, imageUp, imageDown, imageChecked)
                val visImageButtonStyle = VisImageButtonStyle(up, down, checked, imageUp, imageDown, imageChecked)

                lua.getFieldString(3, "over")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    imageButtonStyle.over = drawable
                    visImageButtonStyle.over = drawable
                }

                lua.getFieldString(3, "checkedOver")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    imageButtonStyle.checkedOver = drawable
                    visImageButtonStyle.checkedOver = drawable
                }

                lua.getFieldString(3, "disabled")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    imageButtonStyle.disabled = drawable
                    visImageButtonStyle.disabled = drawable
                }

                lua.getFieldString(3, "imageOver")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    imageButtonStyle.imageOver = drawable
                    visImageButtonStyle.imageOver = drawable
                }

                lua.getFieldString(3, "imageCheckedOver")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    imageButtonStyle.imageCheckedOver = drawable
                    visImageButtonStyle.imageCheckedOver = drawable
                }

                lua.getFieldString(3, "imageDisabled")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    imageButtonStyle.imageDisabled = drawable
                    visImageButtonStyle.imageDisabled = drawable
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
            lua.checkType(3, Lua.LuaType.TABLE)

            try {
                val font = lua.getFieldString(3, "font")?.let {
                    module.resolveFont(delegate, it)
                }
                val fontColor = lua.getFieldString(3, "fontColor")?.let {
                    module.resolveColor(delegate, it)
                } ?: Color.WHITE
                val cursor = lua.getFieldString(3, "cursor")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val selection = lua.getFieldString(3, "selection")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val background = lua.getFieldString(3, "background")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val textFieldStyle = TextField.TextFieldStyle(font, fontColor, cursor, selection, background)
                val visTextFieldStyle = VisTextField.VisTextFieldStyle(font, fontColor, cursor, selection, background)

                lua.getFieldString(3, "focusedFontColor")?.let {
                    val color = module.resolveColor(delegate, it)
                    textFieldStyle.focusedFontColor = color
                    visTextFieldStyle.focusedFontColor = color
                }

                lua.getFieldString(3, "disabledFontColor")?.let {
                    val color = module.resolveColor(delegate, it)
                    textFieldStyle.disabledFontColor = color
                    visTextFieldStyle.disabledFontColor = color
                }

                lua.getFieldString(3, "focusedBackground")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    textFieldStyle.focusedBackground = drawable
                    visTextFieldStyle.focusedBackground = drawable
                }

                lua.getFieldString(3, "disabledBackground")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    textFieldStyle.disabledBackground = drawable
                    visTextFieldStyle.disabledBackground = drawable
                }

                lua.getFieldString(3, "messageFont")?.let {
                    val font = module.resolveFont(delegate, it)
                    textFieldStyle.messageFont = font
                    visTextFieldStyle.messageFont = font
                }

                lua.getFieldString(3, "messageFontColor")?.let {
                    val color = module.resolveColor(delegate, it)
                    textFieldStyle.messageFontColor = color
                    visTextFieldStyle.messageFontColor = color
                }

                delegate.add(styleName, textFieldStyle)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add text field style '$styleName': ${e.message}", e))
            }
        }

        fun AddScrollPaneStyle(lua: Lua): Int {
            val styleName = lua.checkString(2)
            lua.checkType(3, Lua.LuaType.TABLE)

            try {
                val background = lua.getFieldString(3, "background")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val hScroll = lua.getFieldString(3, "hScroll")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val hScrollKnob = lua.getFieldString(3, "hScrollKnob")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val vScroll = lua.getFieldString(3, "vScroll")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val vScrollKnob = lua.getFieldString(3, "vScrollKnob")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val scrollPaneStyle = ScrollPane.ScrollPaneStyle(background, hScroll, hScrollKnob, vScroll, vScrollKnob)
                lua.getFieldString(3, "corner")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    scrollPaneStyle.corner = drawable
                }
                delegate.add(styleName, scrollPaneStyle)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add scroll pane style '$styleName': ${e.message}", e))
            }
        }

        fun AddProgressBarStyle(lua: Lua): Int {
            val styleName = lua.checkString(2)
            lua.checkType(3, Lua.LuaType.TABLE)

            try {
                val background = lua.getFieldString(3, "background")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val knob = lua.getFieldString(3, "knob")?.let {
                    module.resolveDrawable(delegate, it)
                }
                val progressBarStyle = ProgressBar.ProgressBarStyle(background, knob)
                lua.getFieldString(3, "knobBefore")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    progressBarStyle.knobBefore = drawable
                }
                lua.getFieldString(3, "knobAfter")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    progressBarStyle.knobAfter = drawable
                }
                lua.getFieldString(3, "disabledBackground")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    progressBarStyle.disabledBackground = drawable
                }
                lua.getFieldString(3, "disabledKnob")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    progressBarStyle.disabledKnob = drawable
                }
                lua.getFieldString(3, "disabledKnobBefore")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    progressBarStyle.disabledKnobBefore = drawable
                }
                lua.getFieldString(3, "disabledKnobAfter")?.let {
                    val drawable = module.resolveDrawable(delegate, it)
                    progressBarStyle.disabledKnobAfter = drawable
                }
                delegate.add(styleName, progressBarStyle)
                return 0
            } catch (e: Exception) {
                return lua.error(RuntimeException("Failed to add progress bar style '$styleName': ${e.message}", e))
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
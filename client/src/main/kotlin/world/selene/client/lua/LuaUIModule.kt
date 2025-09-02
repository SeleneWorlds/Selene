package world.selene.client.lua

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
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
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
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
import world.selene.client.rendering.visual2d.Visual2D
import world.selene.client.ui.DrawableDrawable
import world.selene.client.ui.ParameterizedActorConsumer
import world.selene.client.ui.SeleneLmlParser
import world.selene.client.ui.UI
import world.selene.client.ui.Visual2DDrawable
import world.selene.common.lua.*

class LuaUIModule(private val ui: UI, private val bundleFileResolver: BundleFileResolver) : LuaModule {
    override val name = "selene.ui.lml"

    override fun initialize(luaManager: LuaManager) {
        val actorMetatable = LuaMappedMetatable(Actor::class) {
            getter(Actor::getName)
            getter(Actor::getParent)
            getter(Actor::getWidth)
            getter(Actor::getHeight)
            setter(Actor::setWidth)
            setter(Actor::setHeight)
            callable("Invalidate") {
                val actor = it.checkSelf()
                if (actor is Layout) actor.invalidate()
                0
            }
            getter("MinWidth") {
                val actor = it.checkSelf()
                it.push((actor as? Layout)?.minWidth ?: 0f)
                1
            }
            getter("MinHeight") {
                val actor = it.checkSelf()
                it.push((actor as? Layout)?.minHeight ?: 0f)
                1
            }
            getter("PreferredWidth") {
                val actor = it.checkSelf()
                it.push((actor as? Layout)?.prefWidth ?: 0f)
                1
            }
            getter("PreferredHeight") {
                val actor = it.checkSelf()
                it.push((actor as? Layout)?.prefHeight ?: 0f)
                1
            }
            getter("MaxWidth") {
                val actor = it.checkSelf()
                it.push((actor as? Layout)?.maxWidth ?: 0f)
                1
            }
            getter("MaxHeight") {
                val actor = it.checkSelf()
                it.push((actor as? Layout)?.maxHeight ?: 0f)
                1
            }

            callable("SetStyle") { it ->
                val actor = it.checkSelf()
                val skin = it.checkUserdata(2, Skin::class)
                val style = it.checkString(3)
                when (actor) {
                    is VisTextField -> {
                        actor.style = skin.get(style, VisTextField.VisTextFieldStyle::class.java)
                    }

                    is LinkLabel -> {
                        actor.style = skin.get(style, LinkLabel.LinkLabelStyle::class.java)
                    }

                    is Label -> {
                        actor.style = skin.get(style, Label.LabelStyle::class.java)
                    }

                    is SelectBox<*> -> {
                        actor.style = skin.get(style, SelectBox.SelectBoxStyle::class.java)
                    }

                    is Slider -> {
                        actor.style = skin.get(style, Slider.SliderStyle::class.java)
                    }

                    is ProgressBar -> {
                        actor.style = skin.get(style, ProgressBar.ProgressBarStyle::class.java)
                    }

                    is TextField -> {
                        actor.style = skin.get(style, TextField.TextFieldStyle::class.java)
                    }

                    is List<*> -> {
                        actor.style = skin.get(style, List.ListStyle::class.java)
                    }

                    is Touchpad -> {
                        actor.style = skin.get(style, Touchpad.TouchpadStyle::class.java)
                    }
                }
                0
            }
            callable("Focus") { lua ->
                val actor = lua.checkSelf()
                actor.stage.keyboardFocus = actor
                0
            }
        }
        luaManager.defineMetatable(Actor::class, actorMetatable)
        val groupMetatable = actorMetatable.extend(Group::class) {
            callable("AddChild") {
                val actor = it.checkSelf()
                val child = it.checkUserdata(2, Actor::class)
                actor.addActor(child)
                0
            }
            callable("AddChildBefore") {
                val actor = it.checkSelf()
                val before = it.checkUserdata<Actor>(2)
                val child = it.checkUserdata<Actor>(3)
                actor.addActorBefore(before, child)
                0
            }
        }
        luaManager.defineMetatable(Group::class, groupMetatable)
        luaManager.defineMetatable(VerticalGroup::class, groupMetatable)
        luaManager.defineMetatable(Container::class, groupMetatable.extend(Container::class) {
            callable("AddChild") {
                @Suppress("UNCHECKED_CAST") val actor = it.checkSelf() as Container<Actor>
                val child = it.checkUserdata(2, Actor::class)
                actor.actor = child
                0
            }
            setter("Width") {
                val container = it.checkSelf()
                val minWidth = it.checkFloat(3)
                container.width(minWidth)
                0
            }
            setter("Height") {
                val container = it.checkSelf()
                val minHeight = it.checkFloat(3)
                container.height(minHeight)
                0
            }
            setter("MinWidth") {
                val container = it.checkSelf()
                val minWidth = it.checkFloat(3)
                container.minWidth(minWidth)
                0
            }
            setter("MinHeight") {
                val container = it.checkSelf()
                val minHeight = it.checkFloat(3)
                container.minHeight(minHeight)
                0
            }
            setter("MaxWidth") {
                val container = it.checkSelf()
                val maxWidth = it.checkFloat(3)
                container.maxWidth(maxWidth)
                0
            }
            setter("MaxHeight") {
                val container = it.checkSelf()
                val maxHeight = it.checkFloat(3)
                container.maxHeight(maxHeight)
                0
            }
        })
        luaManager.defineMetatable(Label::class, actorMetatable.extend(Label::class) {
            getter("Text") {
                val label = it.checkSelf()
                it.push(label.text.toString())
                1
            }
            setter("Text") {
                val label = it.checkSelf()
                val text = it.checkString(3)
                label.setText(text)
                0
            }
        })
        luaManager.defineMetatable(TextField::class, actorMetatable.extend(TextField::class) {
            getter(TextField::getDefaultInputListener, "InputListener")
            getter("Text") {
                val textField = it.checkSelf()
                it.push(textField.text.toString())
                1
            }
            setter("Text") {
                val textField = it.checkSelf()
                val text = it.checkString(3)
                textField.setText(text)
                0
            }
        })
        luaManager.defineMetatable(ImageButton::class, actorMetatable.extend(ImageButton::class) {
            callable("SetStyle") { lua ->
                val actor = lua.checkSelf()
                val skinOrStyle = lua.toJavaObject(2)
                actor.style = if (skinOrStyle is Skin) {
                    val style = lua.checkString(3)
                    skinOrStyle.get(style, ImageButton.ImageButtonStyle::class.java)
                } else skinOrStyle as? ImageButton.ImageButtonStyle
                    ?: return@callable lua.error(IllegalArgumentException("Expected Skin or ImageButtonStyle"))
                0
            }
        })
        luaManager.defineMetatable(VisImageButton::class, actorMetatable.extend(VisImageButton::class) {
            callable("SetStyle") {
                val actor = it.checkSelf()
                val skin = it.checkUserdata(2, Skin::class)
                val style = it.checkString(3)
                actor.style = skin.get(style, VisImageButtonStyle::class.java)
                0
            }
        })
        luaManager.defineMetatable(ProgressBar::class, actorMetatable.extend(ProgressBar::class) {
            getter(ProgressBar::getValue)
            setter(ProgressBar::setValue)
        })
        luaManager.defineMetatable(Skin::class, LuaMappedMetatable(Skin::class) {
            callable("AddTexture") {
                val skin = it.checkSelf()
                val name = it.checkString(2)
                if (it.isString(3)) {
                    val texturePath = it.checkString(3)
                    val textureFile = bundleFileResolver.resolve(texturePath)
                    if (!textureFile.exists()) {
                        return@callable it.error(IllegalArgumentException("Texture file not found: $texturePath"))
                    }

                    val texture = Texture(textureFile)
                    val region = TextureRegion(texture)
                    skin.add(name, region)
                } else if (it.isUserdata(3)) {
                    val texture = it.checkUserdata(3, LuaTexturesModule.LuaTexture::class).texture
                    val region = TextureRegion(texture)
                    skin.add(name, region)
                }
                return@callable 0
            }

            callable("AddButtonStyle") { lua ->
                val skin = lua.checkSelf()
                val styleName = lua.checkString(2)
                lua.checkType(3, Lua.LuaType.TABLE)

                val up = lua.getFieldString(3, "up")?.let {
                    resolveDrawable(skin, it)
                }
                val down = lua.getFieldString(3, "down")?.let {
                    resolveDrawable(skin, it)
                }
                val checked = lua.getFieldString(3, "checked")?.let {
                    resolveDrawable(skin, it)
                }
                val buttonStyle = Button.ButtonStyle(up, down, checked)
                lua.getFieldString(3, "over")?.let {
                    buttonStyle.over = resolveDrawable(skin, it)
                }
                // TODO many more fields here to support
                skin.add(styleName, buttonStyle)
                return@callable 0
            }

            callable("AddLabelStyle") { lua ->
                val skin = lua.checkSelf()
                val styleName = lua.checkString(2)
                lua.checkType(3, Lua.LuaType.TABLE)

                val font = lua.getFieldString(3, "font")?.let {
                    resolveFont(skin, it)
                }
                val fontColor = lua.getFieldString(3, "fontColor")?.let {
                    resolveColor(skin, it)
                } ?: Color.WHITE
                val labelStyle = Label.LabelStyle(font, fontColor)
                lua.getFieldString(3, "background")?.let {
                    labelStyle.background = resolveDrawable(skin, it)
                }
                skin.add(styleName, labelStyle)
                return@callable 0
            }

            callable("AddImageButtonStyle") { lua ->
                val skin = lua.checkSelf()
                val styleName = lua.checkString(2)
                val styles = createImageButtonStyle(lua, 3, skin)
                for (style in styles) {
                    skin.add(styleName, style)
                }
                return@callable 0
            }

            callable("AddTextFieldStyle") { lua ->
                val skin = lua.checkSelf()
                val styleName = lua.checkString(2)
                lua.checkType(3, Lua.LuaType.TABLE)

                val font = lua.getFieldString(3, "font")?.let {
                    resolveFont(skin, it)
                }
                val fontColor = lua.getFieldString(3, "fontColor")?.let {
                    resolveColor(skin, it)
                } ?: Color.WHITE
                val cursor = lua.getFieldString(3, "cursor")?.let {
                    resolveDrawable(skin, it)
                }
                val selection = lua.getFieldString(3, "selection")?.let {
                    resolveDrawable(skin, it)
                }
                val background = lua.getFieldString(3, "background")?.let {
                    resolveDrawable(skin, it)
                }
                val textFieldStyle = TextField.TextFieldStyle(font, fontColor, cursor, selection, background)
                val visTextFieldStyle = VisTextField.VisTextFieldStyle(font, fontColor, cursor, selection, background)

                lua.getFieldString(3, "focusedFontColor")?.let {
                    val color = resolveColor(skin, it)
                    textFieldStyle.focusedFontColor = color
                    visTextFieldStyle.focusedFontColor = color
                }

                lua.getFieldString(3, "disabledFontColor")?.let {
                    val color = resolveColor(skin, it)
                    textFieldStyle.disabledFontColor = color
                    visTextFieldStyle.disabledFontColor = color
                }

                lua.getFieldString(3, "focusedBackground")?.let {
                    val drawable = resolveDrawable(skin, it)
                    textFieldStyle.focusedBackground = drawable
                    visTextFieldStyle.focusedBackground = drawable
                }

                lua.getFieldString(3, "disabledBackground")?.let {
                    val drawable = resolveDrawable(skin, it)
                    textFieldStyle.disabledBackground = drawable
                    visTextFieldStyle.disabledBackground = drawable
                }

                lua.getFieldString(3, "messageFont")?.let {
                    val font = resolveFont(skin, it)
                    textFieldStyle.messageFont = font
                    visTextFieldStyle.messageFont = font
                }

                lua.getFieldString(3, "messageFontColor")?.let {
                    val color = resolveColor(skin, it)
                    textFieldStyle.messageFontColor = color
                    visTextFieldStyle.messageFontColor = color
                }

                skin.add(styleName, textFieldStyle)
                return@callable 0
            }

            callable("AddScrollPaneStyle") { lua ->
                val skin = lua.checkSelf()
                val styleName = lua.checkString(2)
                lua.checkType(3, Lua.LuaType.TABLE)

                val background = lua.getFieldString(3, "background")?.let {
                    resolveDrawable(skin, it)
                }
                val hScroll = lua.getFieldString(3, "hScroll")?.let {
                    resolveDrawable(skin, it)
                }
                val hScrollKnob = lua.getFieldString(3, "hScrollKnob")?.let {
                    resolveDrawable(skin, it)
                }
                val vScroll = lua.getFieldString(3, "vScroll")?.let {
                    resolveDrawable(skin, it)
                }
                val vScrollKnob = lua.getFieldString(3, "vScrollKnob")?.let {
                    resolveDrawable(skin, it)
                }
                val scrollPaneStyle = ScrollPane.ScrollPaneStyle(background, hScroll, hScrollKnob, vScroll, vScrollKnob)
                lua.getFieldString(3, "corner")?.let {
                    val drawable = resolveDrawable(skin, it)
                    scrollPaneStyle.corner = drawable
                }
                skin.add(styleName, scrollPaneStyle)
                return@callable 0
            }

            callable("AddProgressBarStyle") { lua ->
                val skin = lua.checkSelf()
                val styleName = lua.checkString(2)
                lua.checkType(3, Lua.LuaType.TABLE)

                val background = lua.getFieldString(3, "background")?.let {
                    resolveDrawable(skin, it)
                }
                val knob = lua.getFieldString(3, "knob")?.let {
                    resolveDrawable(skin, it)
                }
                val progressBarStyle = ProgressBar.ProgressBarStyle(background, knob)
                lua.getFieldString(3, "knobBefore")?.let {
                    val drawable = resolveDrawable(skin, it)
                    progressBarStyle.knobBefore = drawable
                }
                lua.getFieldString(3, "knobAfter")?.let {
                    val drawable = resolveDrawable(skin, it)
                    progressBarStyle.knobAfter = drawable
                }
                lua.getFieldString(3, "disabledBackground")?.let {
                    val drawable = resolveDrawable(skin, it)
                    progressBarStyle.disabledBackground = drawable
                }
                lua.getFieldString(3, "disabledKnob")?.let {
                    val drawable = resolveDrawable(skin, it)
                    progressBarStyle.disabledKnob = drawable
                }
                lua.getFieldString(3, "disabledKnobBefore")?.let {
                    val drawable = resolveDrawable(skin, it)
                    progressBarStyle.disabledKnobBefore = drawable
                }
                lua.getFieldString(3, "disabledKnobAfter")?.let {
                    val drawable = resolveDrawable(skin, it)
                    progressBarStyle.disabledKnobAfter = drawable
                }
                skin.add(styleName, progressBarStyle)
                return@callable 0
            }
        })
        luaManager.defineMetatable(
            TextField.TextFieldClickListener::class,
            LuaMappedMetatable(TextField.TextFieldClickListener::class) {
                callable("KeyDown") { lua ->
                    val listener = lua.checkSelf()
                    val event = lua.checkUserdata<InputEvent>(2)
                    val keyCode = lua.checkInt(3)
                    lua.push(listener.keyDown(event, keyCode))
                    1
                }
                callable("KeyUp") { lua ->
                    val listener = lua.checkSelf()
                    val event = lua.checkUserdata<InputEvent>(2)
                    val keyCode = lua.checkInt(3)
                    lua.push(listener.keyUp(event, keyCode))
                    1
                }
                callable("KeyTyped") { lua ->
                    val listener = lua.checkSelf()
                    val event = lua.checkUserdata<InputEvent>(2)
                    val char = lua.checkInt(3).toChar()
                    lua.push(listener.keyTyped(event, char))
                    1
                }
            })
    }

    override fun register(table: LuaValue) {
        table.register("LoadUI", this::luaLoadUI)
        table.register("LoadSkin", this::luaLoadSkin)
        table.register("CreateSkin", this::luaCreateSkin)
        table.register("CreateContainer", this::luaCreateContainer)
        table.register("CreateLabel", this::luaCreateLabel)
        table.register("AddToRoot", this::luaAddToRoot)
        table.register("SetFocus", this::luaSetFocus)
        table.register("GetFocus", this::luaGetFocus)
        table.register("CreateImageButtonStyle", this::luaCreateImageButtonStyle)
        table.register("AddInputProcessor", this::luaAddInputProcessor)
        table.set("Root", ui.bundlesRoot)
    }

    private fun luaAddInputProcessor(lua: Lua): Int {
        lua.checkType(1, Lua.LuaType.TABLE)

        val keyUp = lua.getFieldFunction(1, "KeyUp")
        val keyDown = lua.getFieldFunction(1, "KeyDown")
        val keyTyped = lua.getFieldFunction(1, "KeyTyped")

        val registrationSite = lua.getCallerInfo()
        ui.stage.addListener { event ->
            if (event is InputEvent) {
                return@addListener when (event.type) {
                    InputEvent.Type.keyUp -> {
                        if (keyUp != null) {
                            lua.push(keyUp)
                            lua.push(event, Lua.Conversion.NONE)
                            lua.push(event.keyCode, Lua.Conversion.FULL)
                            lua.xpCall(
                                2,
                                1,
                                ClosureTrace { "[ui keyUp \"${Input.Keys.toString(event.keyCode)}\"] registered in $registrationSite" })
                            lua.toBoolean(-1)
                        } else false
                    }

                    InputEvent.Type.keyDown -> {
                        if (keyDown != null) {
                            lua.push(keyDown)
                            lua.push(event, Lua.Conversion.NONE)
                            lua.push(event.keyCode, Lua.Conversion.FULL)
                            lua.xpCall(
                                2,
                                1,
                                ClosureTrace { "[ui keyDown \"${Input.Keys.toString(event.keyCode)}\"] registered in $registrationSite" })
                            lua.toBoolean(-1)
                        } else false
                    }

                    InputEvent.Type.keyTyped -> {
                        if (keyTyped != null) {
                            lua.push(keyTyped)
                            lua.push(event, Lua.Conversion.NONE)
                            lua.push(event.character, Lua.Conversion.FULL)
                            lua.xpCall(
                                2,
                                1,
                                ClosureTrace { "[ui keyTyped \"${Input.Keys.toString(event.keyCode)}\"] registered in $registrationSite" })
                            lua.toBoolean(-1)
                        } else false
                    }

                    else -> false
                }
            }
            false
        }
        return 0
    }

    private fun luaSetFocus(lua: Lua): Int {
        val actor = if (lua.isUserdata(1)) lua.checkUserdata<Actor>(1) else null
        ui.stage.keyboardFocus = actor
        return 0
    }

    private fun luaGetFocus(lua: Lua): Int {
        val actor = ui.stage.keyboardFocus
        if (actor != null) {
            lua.push(actor, Lua.Conversion.NONE)
        } else {
            lua.pushNil()
        }
        return 1
    }

    private fun luaAddToRoot(lua: Lua): Int {
        if (lua.isTable(1)) {
            val actors = lua.toAnyMap(1)?.values ?: emptyList()
            for (actor in actors) {
                if (actor is Actor) {
                    ui.bundlesRoot.add(actor)
                }
            }
        } else if (lua.isUserdata(1)) {
            val actor = lua.checkUserdata(1, Actor::class)
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
        val skin = lua.getFieldUserdata(2, "skin", Skin::class)

        if (lua.isTable(2)) {
            lua.getField(2, "actions")
            if (lua.isTable(-1)) {
                lua.toTypedMap<String, LuaValue>(-1)?.entries?.forEach { (actionName, actionFunction) ->
                    actions[actionName] = actionFunction
                }
            }
            lua.pop(1)
        }

        val registrationSite = lua.getCallerInfo()

        try {
            val parser = SeleneLmlParser.parser().skin(skin ?: ui.systemSkin)

            // Register actions from Lua
            for ((actionName, actionFunction) in actions) {
                parser.action(actionName, object : ParameterizedActorConsumer<Any?, Actor> {
                    override fun consumeWithParameters(actor: Actor, vararg parameters: Any): Any? {
                        lua.push(actionFunction, Lua.Conversion.NONE)
                        lua.push(actor, Lua.Conversion.NONE)
                        for (parameter in parameters) {
                            lua.push(parameter, Lua.Conversion.FULL)
                        }
                        lua.xpCall(
                            parameters.size + 1,
                            1,
                            ClosureTrace { "[ui action \"$actionName\"] registered in $registrationSite" })
                        return lua.toAny(-1)
                    }
                })
            }

            // Setup i18n bundle
            val i18nFileHandle = bundleFileResolver.resolve(i18nBundle)
            val i18nBundle = I18NBundle.createBundle(i18nFileHandle)
            parser.i18nBundle(i18nBundle)

            // Parse the UI XML file
            val xmlFile = bundleFileResolver.resolve(xmlFilePath)
            if (!xmlFile.exists()) {
                return lua.error(IllegalArgumentException("XML file not found: $xmlFilePath"))
            }

            val actors = parser.build().parseTemplate(xmlFile)

            val actorsByName = mutableMapOf<String, Actor>()
            fun collectActorsByName(actor: Actor) {
                if (actor.name != null && actor.name.isNotEmpty()) {
                    actorsByName[actor.name] = actor
                }
                if (actor is Group) {
                    actor.children.forEach { child ->
                        collectActorsByName(child)
                    }
                }
            }
            actors.forEach { collectActorsByName(it) }

            lua.push(actors.toList(), Lua.Conversion.FULL)
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
            lua.push(skin, Lua.Conversion.NONE)
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
            add("hidden", ImageButton.ImageButtonStyle())
        }
        lua.push(skin, Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateContainer(lua: Lua): Int {
        val skin = lua.checkUserdata(1, Skin::class)
        if (lua.top > 1) {
            lua.checkType(2, Lua.LuaType.TABLE)
        }

        val child = lua.getFieldUserdata(2, "child", Actor::class)
        val container = Container<Actor>(child)
        lua.getFieldString(2, "background")?.let {
            container.background = resolveDrawable(skin, lua.toString(-1)!!)
        }
        lua.getFieldFloat(2, "width")?.let {
            container.width(it)
        }
        lua.getFieldFloat(2, "height")?.let {
            container.height(it)
        }
        lua.push(container, Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateLabel(lua: Lua): Int {
        val skin = lua.checkUserdata(1, Skin::class)
        if (lua.top > 1) {
            lua.checkType(2, Lua.LuaType.TABLE)
        }

        try {
            val style = lua.getFieldString(2, "style") ?: "default"
            val labelStyle = skin.get(style, Label.LabelStyle::class.java)
            val text = lua.getFieldString(2, "text") ?: ""
            val label = Label(text, labelStyle)
            label.wrap = lua.getFieldBoolean(2, "wrap") ?: false
            lua.push(label, Lua.Conversion.NONE)
            return 1
        } catch (e: Exception) {
            return lua.error(RuntimeException("Failed to create label: ${e.message}", e))
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

    private fun resolveDrawable(skin: Skin?, path: String): Drawable? {
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

    private fun luaCreateImageButtonStyle(lua: Lua): Int {
        val skin = lua.toUserdata<Skin>(2)
        val styles = createImageButtonStyle(lua, 1, skin)
        for (style in styles) {
            lua.push(style, Lua.Conversion.NONE)
        }
        return styles.size
    }

    private fun createImageButtonStyle(
        lua: Lua,
        tableIndex: Int,
        skin: Skin? = null
    ): kotlin.collections.List<Button.ButtonStyle> {
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
        val visImageButtonStyle = VisImageButtonStyle(up, down, checked, imageUp, imageDown, imageChecked)

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
package world.selene.client.lua

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.I18NBundle
import com.kotcrab.vis.ui.widget.VisImageButton
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.assets.BundleFileResolver
import world.selene.client.ui.ParameterizedActorConsumer
import world.selene.client.ui.SeleneLmlParser
import world.selene.client.ui.UI
import world.selene.common.lua.*

/**
 * Load, skin and manipulate UIs.
 */
@Suppress("SameReturnValue")
class LuaUIModule(
    private val ui: UI,
    private val bundleFileResolver: BundleFileResolver,
    private val luaSkinUtils: LuaSkinUtils,
    private val skinLuaMetatable: SkinLuaMetatable
) : LuaModule {
    override val name = "selene.ui.lml"

    private val bundlesRoot: Stack = ui.bundlesRoot

    override fun initialize(luaManager: LuaManager) {
        luaManager.defineMetatable(Actor::class, ActorLuaMetatable.luaMeta)
        luaManager.defineMetatable(Group::class, GroupLuaMetatable.luaMeta)
        luaManager.defineMetatable(VerticalGroup::class, GroupLuaMetatable.luaMeta)
        luaManager.defineMetatable(Container::class, ContainerLuaMetatable.luaMeta)
        luaManager.defineMetatable(Label::class, LabelLuaMetatable.luaMeta)
        luaManager.defineMetatable(TextField::class, TextFieldLuaMetatable.luaMeta)
        luaManager.defineMetatable(ImageButton::class, ImageButtonLuaMetatable.luaMeta)
        luaManager.defineMetatable(VisImageButton::class, VisImageButtonLuaMetatable.luaMeta)
        luaManager.defineMetatable(ProgressBar::class, ProgressBarLuaMetatable.luaMeta)
        luaManager.defineMetatable(Skin::class, skinLuaMetatable.luaMeta)
        luaManager.defineMetatable(TextField.TextFieldClickListener::class, TextFieldClickListenerLuaMetatable.luaMeta)
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
        table.set("Root", bundlesRoot)
    }

    /**
     * Adds an input processor for handling keyboard events to the UI.
     * The processor table can contain KeyUp, KeyDown, and KeyTyped functions.
     *
     * ```signatures
     * AddInputProcessor(processor: table{KeyUp: function(event: InputEvent, keyCode: number), KeyDown: function(event: InputEvent, keyCode: number), KeyTyped: function(event: InputEvent, char: number)}})
     * ```
     */
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

    /**
     * Sets keyboard focus to the specified actor, or clears focus if nil.
     *
     * ```signatures
     * SetFocus(actor: Actor|nil)
     * ```
     */
    private fun luaSetFocus(lua: Lua): Int {
        val actor = if (lua.isUserdata(1)) lua.checkUserdata<Actor>(1) else null
        ui.stage.keyboardFocus = actor
        return 0
    }

    /**
     * Returns the actor with keyboard focus, or `nil` if there is none.
     *
     * ```signatures
     * GetFocus() -> Actor|nil
     * ```
     */
    private fun luaGetFocus(lua: Lua): Int {
        val actor = ui.stage.keyboardFocus
        if (actor != null) {
            lua.push(actor, Lua.Conversion.NONE)
        } else {
            lua.pushNil()
        }
        return 1
    }

    /**
     * Adds actors to the root UI container. Accepts a single actor or table of actors.
     *
     * ```signatures
     * AddToRoot(actors: Actor|table[Actor])
     * ```
     */
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

    /**
     * Loads a UI from an XML file with optional actions and configuration.
     * Returns an array of root actors and a table of actors by name.
     * Throws an error if the XML file is not found.
     *
     * ```signatures
     * LoadUI(xmlFilePath: string, config: table{i18nBundle: string, skin: Skin, actions: table[string, function(actor: Actor)]}) -> actors: table[Actor], actorsByName: table[string, Actor]
     * ```
     */
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

    /**
     * Loads a skin from a JSON file.
     * Throws an error if the skin file is not found.
     *
     * ```signatures
     * LoadSkin(skinPath: string) -> Skin
     * ```
     */
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

    /**
     * Creates a new empty skin with default font and styles.
     *
     * ```signatures
     * CreateSkin() -> Skin
     * ```
     */
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

    /**
     * Creates a container actor with optional child and styling.
     *
     * ```signatures
     * CreateContainer(skin: Skin, config: table{child: Actor, background: string, width: number, height: number}) -> Container
     * ```
     */
    private fun luaCreateContainer(lua: Lua): Int {
        val skin = lua.checkUserdata(1, Skin::class)
        if (lua.top > 1) {
            lua.checkType(2, Lua.LuaType.TABLE)
        }

        val child = lua.getFieldUserdata(2, "child", Actor::class)
        val container = Container<Actor>(child)
        lua.getFieldString(2, "background")?.let {
            container.background = luaSkinUtils.resolveDrawable(skin, lua.toString(-1)!!)
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

    /**
     * Creates a label actor with text and styling.
     * Throws an error if the label style is not found in the skin.
     *
     * ```signatures
     * CreateLabel(skin: Skin, config: table{style: string, text: string, wrap: boolean}) -> Label
     * ```
     */
    private fun luaCreateLabel(lua: Lua): Int {
        val skin = lua.checkUserdata(1, Skin::class)
        if (lua.top > 1) {
            lua.checkType(2, Lua.LuaType.TABLE)
        }

        val style = lua.getFieldString(2, "style") ?: "default"
        val labelStyle = skin.get(style, Label.LabelStyle::class.java)
        val text = lua.getFieldString(2, "text") ?: ""
        val label = Label(text, labelStyle)
        label.wrap = lua.getFieldBoolean(2, "wrap") ?: false
        lua.push(label, Lua.Conversion.NONE)
        return 1
    }

    /**
     * Creates image button styles from configuration table.
     * Returns both ImageButtonStyle and VisImageButtonStyle.
     *
     * ```signatures
     * CreateImageButtonStyle(config: table) -> ImageButtonStyle, VisImageButtonStyle
     * CreateImageButtonStyle(config: table, skin: Skin) -> ImageButtonStyle, VisImageButtonStyle
     * ```
     */
    private fun luaCreateImageButtonStyle(lua: Lua): Int {
        val skin = lua.toUserdata<Skin>(2)
        val styles = luaSkinUtils.createImageButtonStyle(lua, 1, skin)
        for (style in styles) {
            lua.push(style, Lua.Conversion.NONE)
        }
        return styles.size
    }

}
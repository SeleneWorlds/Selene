package world.selene.client.ui.lua

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.utils.I18NBundle
import com.kotcrab.vis.ui.widget.Draggable
import com.kotcrab.vis.ui.widget.VisImageButton
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.bundles.BundleFileResolver
import world.selene.client.ui.ParameterizedActorConsumer
import world.selene.client.ui.UI
import world.selene.client.ui.lml.SeleneLmlParser
import world.selene.common.lua.ClosureTrace
import world.selene.common.lua.LuaManager
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkType
import world.selene.common.lua.util.checkUserdata
import world.selene.common.lua.util.getCallerInfo
import world.selene.common.lua.util.getFieldBoolean
import world.selene.common.lua.util.getFieldFloat
import world.selene.common.lua.util.getFieldFunction
import world.selene.common.lua.util.getFieldString
import world.selene.common.lua.util.getFieldUserdata
import world.selene.common.lua.util.toAny
import world.selene.common.lua.util.toAnyMap
import world.selene.common.lua.util.toTypedMap
import world.selene.common.lua.util.toUserdata
import world.selene.common.lua.util.xpCall

/**
 * Load, skin and manipulate UIs.
 */
@Suppress("SameReturnValue")
class UIApi(
    private val ui: UI,
    private val bundleFileResolver: BundleFileResolver,
    private val luaSkinUtils: LuaSkinUtils,
    private val skinLuaMetatable: SkinLuaMetatable
) {
    val bundlesRoot: Stack = ui.bundlesRoot

    fun initialize(luaManager: LuaManager) {
        luaManager.defineMetatable(Stage::class, StageLuaMetatable.luaMeta)
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

    fun luaAddInputProcessor(lua: Lua): Int {
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
                        } else {
                            false
                        }
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
                        } else {
                            false
                        }
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
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            }
            false
        }
        return 0
    }

    fun luaSetFocus(lua: Lua): Int {
        val actor = if (lua.isUserdata(1)) lua.checkUserdata<Actor>(1) else null
        ui.stage.keyboardFocus = actor
        return 0
    }

    fun luaGetFocus(lua: Lua): Int {
        val actor = ui.stage.keyboardFocus
        if (actor != null) {
            lua.push(actor, Lua.Conversion.NONE)
        } else {
            lua.pushNil()
        }
        return 1
    }

    fun luaAddToRoot(lua: Lua): Int {
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

    fun luaLoadUI(lua: Lua): Int {
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

            for ((actionName, actionFunction) in actions) {
                parser.action(actionName, object : ParameterizedActorConsumer<Any?, Any> {
                    override fun consumeWithParameters(widget: Any, vararg parameters: Any): Any? {
                        lua.push(actionFunction, Lua.Conversion.NONE)
                        lua.push(widget, Lua.Conversion.NONE)
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

            val i18nFileHandle = bundleFileResolver.resolve(i18nBundle)
            val bundle = I18NBundle.createBundle(i18nFileHandle)
            parser.i18nBundle(bundle)

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

    fun luaLoadSkin(lua: Lua): Int {
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

    fun luaCreateSkin(lua: Lua): Int {
        val font = BitmapFont()
        val skin = Skin().apply {
            add("default", font)
            add("default", Label.LabelStyle(font, Color.WHITE))
            add("hidden", ImageButton.ImageButtonStyle())
        }
        lua.push(skin, Lua.Conversion.NONE)
        return 1
    }

    fun luaCreateContainer(lua: Lua): Int {
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

    fun luaCreateLabel(lua: Lua): Int {
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

    fun luaCreateButtonStyle(lua: Lua): Int {
        val skin = lua.toUserdata<Skin>(2)
        val styles = luaSkinUtils.createButtonStyle(lua, 1, skin)
        for (style in styles) {
            lua.push(style, Lua.Conversion.NONE)
        }
        return styles.size
    }

    fun luaCreateImageButtonStyle(lua: Lua): Int {
        val skin = lua.toUserdata<Skin>(2)
        val styles = luaSkinUtils.createImageButtonStyle(lua, 1, skin)
        for (style in styles) {
            lua.push(style, Lua.Conversion.NONE)
        }
        return styles.size
    }

    fun luaCreateDragListener(lua: Lua): Int {
        lua.checkType(1, Lua.LuaType.TABLE)

        val onStart = lua.getFieldFunction(1, "onStart")
        val onDrag = lua.getFieldFunction(1, "onDrag")
        val onEnd = lua.getFieldFunction(1, "onEnd")
        val registrationSite = lua.getCallerInfo()
        val dragListener = object : Draggable.DragListener {
            override fun onStart(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean {
                if (onStart != null) {
                    lua.push(onStart)
                    lua.push(draggable, Lua.Conversion.NONE)
                    lua.push(actor, Lua.Conversion.NONE)
                    lua.push(stageX)
                    lua.push(stageY)
                    lua.xpCall(4, 1, ClosureTrace { "[drag listener] created in $registrationSite" })
                    return lua.toBoolean(-1)
                }
                return true
            }

            override fun onDrag(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float) {
                if (onDrag != null) {
                    lua.push(onDrag)
                    lua.push(draggable, Lua.Conversion.NONE)
                    lua.push(actor, Lua.Conversion.NONE)
                    lua.push(stageX)
                    lua.push(stageY)
                    lua.xpCall(4, 0, ClosureTrace { "[drag listener] created in $registrationSite" })
                }
            }

            override fun onEnd(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean {
                if (onEnd != null) {
                    lua.push(onEnd)
                    lua.push(draggable, Lua.Conversion.NONE)
                    lua.push(actor, Lua.Conversion.NONE)
                    lua.push(stageX)
                    lua.push(stageY)
                    lua.xpCall(4, 1, ClosureTrace { "[drag listener] created in $registrationSite" })
                    return lua.toBoolean(-1)
                }
                return true
            }
        }

        lua.push(dragListener, Lua.Conversion.NONE)
        return 1
    }
}

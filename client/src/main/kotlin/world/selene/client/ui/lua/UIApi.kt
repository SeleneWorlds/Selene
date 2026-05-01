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
import world.selene.common.lua.util.CallerInfo
import world.selene.common.lua.util.toAny
import world.selene.common.lua.util.xpCall

/**
 * Load, skin and manipulate UIs.
 */
@Suppress("SameReturnValue")
class UIApi(
    private val ui: UI,
    private val bundleFileResolver: BundleFileResolver,
    internal val luaSkinUtils: LuaSkinUtils,
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

    fun addInputProcessor(
        keyUp: LuaValue?,
        keyDown: LuaValue?,
        keyTyped: LuaValue?,
        registrationSite: CallerInfo
    ) {
        ui.stage.addListener { event ->
            if (event is InputEvent) {
                return@addListener when (event.type) {
                    InputEvent.Type.keyUp -> {
                        if (keyUp != null) {
                            val callbackLua = keyUp.state()
                            callbackLua.push(keyUp)
                            callbackLua.push(event, Lua.Conversion.NONE)
                            callbackLua.push(event.keyCode, Lua.Conversion.FULL)
                            callbackLua.xpCall(
                                2,
                                1,
                                ClosureTrace { "[ui keyUp \"${Input.Keys.toString(event.keyCode)}\"] registered in $registrationSite" })
                            callbackLua.toBoolean(-1)
                        } else {
                            false
                        }
                    }

                    InputEvent.Type.keyDown -> {
                        if (keyDown != null) {
                            val callbackLua = keyDown.state()
                            callbackLua.push(keyDown)
                            callbackLua.push(event, Lua.Conversion.NONE)
                            callbackLua.push(event.keyCode, Lua.Conversion.FULL)
                            callbackLua.xpCall(
                                2,
                                1,
                                ClosureTrace { "[ui keyDown \"${Input.Keys.toString(event.keyCode)}\"] registered in $registrationSite" })
                            callbackLua.toBoolean(-1)
                        } else {
                            false
                        }
                    }

                    InputEvent.Type.keyTyped -> {
                        if (keyTyped != null) {
                            val callbackLua = keyTyped.state()
                            callbackLua.push(keyTyped)
                            callbackLua.push(event, Lua.Conversion.NONE)
                            callbackLua.push(event.character, Lua.Conversion.FULL)
                            callbackLua.xpCall(
                                2,
                                1,
                                ClosureTrace { "[ui keyTyped \"${Input.Keys.toString(event.keyCode)}\"] registered in $registrationSite" })
                            callbackLua.toBoolean(-1)
                        } else {
                            false
                        }
                    }

                    else -> false
                }
            }
            false
        }
    }

    fun setFocus(actor: Actor?) {
        ui.stage.keyboardFocus = actor
    }

    fun getFocus(): Actor? = ui.stage.keyboardFocus

    fun addToRoot(actors: Iterable<Actor>) {
        for (actor in actors) {
            ui.bundlesRoot.add(actor)
        }
    }

    fun loadUI(
        xmlFilePath: String,
        i18nBundle: String,
        skin: Skin?,
        actions: Map<String, LuaValue>,
        registrationSite: CallerInfo
    ): Pair<List<Actor>, Map<String, Actor>> {
        val parser = SeleneLmlParser.parser().skin(skin ?: ui.systemSkin)

        for ((actionName, actionFunction) in actions) {
            parser.action(actionName, object : ParameterizedActorConsumer<Any?, Any> {
                override fun consumeWithParameters(widget: Any, vararg parameters: Any): Any? {
                    val callbackLua = actionFunction.state()
                    callbackLua.push(actionFunction, Lua.Conversion.NONE)
                    callbackLua.push(widget, Lua.Conversion.NONE)
                    for (parameter in parameters) {
                        callbackLua.push(parameter, Lua.Conversion.FULL)
                    }
                    callbackLua.xpCall(
                        parameters.size + 1,
                        1,
                        ClosureTrace { "[ui action \"$actionName\"] registered in $registrationSite" })
                    return callbackLua.toAny(-1)
                }
            })
        }

        val i18nFileHandle = bundleFileResolver.resolve(i18nBundle)
        val bundle = I18NBundle.createBundle(i18nFileHandle)
        parser.i18nBundle(bundle)

        val xmlFile = bundleFileResolver.resolve(xmlFilePath)
        if (!xmlFile.exists()) {
            throw IllegalArgumentException("XML file not found: $xmlFilePath")
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
        return actors.toList() to actorsByName
    }

    fun loadSkin(skinPath: String): Skin {
        val skinFile = bundleFileResolver.resolve(skinPath)
        if (!skinFile.exists()) {
            throw IllegalArgumentException("Skin file not found: $skinPath")
        }
        return Skin(skinFile)
    }

    fun createSkin(): Skin {
        val font = BitmapFont()
        return Skin().apply {
            add("default", font)
            add("default", Label.LabelStyle(font, Color.WHITE))
            add("hidden", ImageButton.ImageButtonStyle())
        }
    }

    fun createContainer(
        skin: Skin,
        child: Actor?,
        background: String?,
        width: Float?,
        height: Float?
    ): Container<Actor> {
        val container = Container<Actor>(child)
        background?.let {
            container.background = luaSkinUtils.resolveDrawable(skin, it)
        }
        width?.let {
            container.width(it)
        }
        height?.let {
            container.height(it)
        }
        return container
    }

    fun createLabel(skin: Skin, style: String, text: String, wrap: Boolean): Label {
        val labelStyle = skin.get(style, Label.LabelStyle::class.java)
        val label = Label(text, labelStyle)
        label.wrap = wrap
        return label
    }

    fun createDragListener(
        onStart: LuaValue?,
        onDrag: LuaValue?,
        onEnd: LuaValue?,
        registrationSite: CallerInfo
    ): Draggable.DragListener {
        return object : Draggable.DragListener {
            override fun onStart(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean {
                if (onStart != null) {
                    val callbackLua = onStart.state()
                    callbackLua.push(onStart)
                    callbackLua.push(draggable, Lua.Conversion.NONE)
                    callbackLua.push(actor, Lua.Conversion.NONE)
                    callbackLua.push(stageX)
                    callbackLua.push(stageY)
                    callbackLua.xpCall(4, 1, ClosureTrace { "[drag listener] created in $registrationSite" })
                    return callbackLua.toBoolean(-1)
                }
                return true
            }

            override fun onDrag(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float) {
                if (onDrag != null) {
                    val callbackLua = onDrag.state()
                    callbackLua.push(onDrag)
                    callbackLua.push(draggable, Lua.Conversion.NONE)
                    callbackLua.push(actor, Lua.Conversion.NONE)
                    callbackLua.push(stageX)
                    callbackLua.push(stageY)
                    callbackLua.xpCall(4, 0, ClosureTrace { "[drag listener] created in $registrationSite" })
                }
            }

            override fun onEnd(draggable: Draggable, actor: Actor, stageX: Float, stageY: Float): Boolean {
                if (onEnd != null) {
                    val callbackLua = onEnd.state()
                    callbackLua.push(onEnd)
                    callbackLua.push(draggable, Lua.Conversion.NONE)
                    callbackLua.push(actor, Lua.Conversion.NONE)
                    callbackLua.push(stageX)
                    callbackLua.push(stageY)
                    callbackLua.xpCall(4, 1, ClosureTrace { "[drag listener] created in $registrationSite" })
                    return callbackLua.toBoolean(-1)
                }
                return true
            }
        }
    }
}

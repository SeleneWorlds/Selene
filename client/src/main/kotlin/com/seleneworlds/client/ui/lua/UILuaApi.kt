package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.kotcrab.vis.ui.widget.VisImageButton
import com.seleneworlds.client.game.ClientEvents
import com.seleneworlds.client.rendering.visual2d.Visual2D
import com.seleneworlds.client.ui.HudApi
import com.seleneworlds.client.ui.ThemeApi
import com.seleneworlds.client.ui.ThemeDefinition
import com.seleneworlds.client.ui.UIApi
import com.seleneworlds.client.ui.drawable.DrawableDrawable
import com.seleneworlds.client.ui.drawable.Visual2DDrawable
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.*
import com.seleneworlds.common.script.ConstantTrace
import com.seleneworlds.common.script.ExposedApi
import com.seleneworlds.common.serialization.seleneJson
import com.seleneworlds.common.serialization.toJsonElement
import com.seleneworlds.common.threading.MainThreadDispatcher
import ktx.async.schedule
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import java.util.concurrent.CompletableFuture

/**
 * Load, skin and manipulate UIs.
 */
class UILuaApi(
    override val api: UIApi,
    private val mainThreadDispatcher: MainThreadDispatcher
) : LuaModule, ExposedApi<UIApi> {
    override val name = "selene.ui.lml"

    override fun initialize(luaManager: LuaManager) {
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
        luaManager.defineMetatable(ThemeApi::class, ThemeLuaApi(this).luaMeta)
        luaManager.defineMetatable(HudApi::class, HudLuaApi.luaMeta)
        luaManager.defineMetatable(TextField.TextFieldClickListener::class, TextFieldClickListenerLuaMetatable.luaMeta)
    }

    override fun register(table: LuaValue) {
        table.register("LoadUI", ::luaLoadUI)
        table.register("LoadTheme", ::luaLoadTheme)
        table.register("CreateTheme", ::luaCreateTheme)
        table.register("CreateContainer", ::luaCreateContainer)
        table.register("CreateLabel", ::luaCreateLabel)
        table.register("AddToRoot", ::luaAddToRoot)
        table.register("SetFocus", ::luaSetFocus)
        table.register("GetFocus", ::luaGetFocus)
        table.register("CreateImageButtonStyle", ::luaCreateImageButtonStyle)
        table.register("CreateButtonStyle", ::luaCreateButtonStyle)
        table.register("AddInputProcessor", ::luaAddInputProcessor)
        table.register("CreateDragListener", ::luaCreateDragListener)
        table.register("CreateAtlas", ::luaCreateAtlas)
        table.set("Root", api.bundlesRoot)
        table.set("Setup", setup)
    }

    private fun luaCreateAtlas(lua: Lua): Int {
        val textures = lua.checkSerializedMap(1)
        lua.push(api.createAtlas(textures), Lua.Conversion.NONE)
        return 1
    }

    private fun luaAddInputProcessor(lua: Lua): Int {
        lua.checkType(1, Lua.LuaType.TABLE)
        val registrationSite = lua.getCallerInfo()
        api.addInputProcessor(
            keyUp = lua.getFieldFunction(1, "KeyUp")?.let { callback ->
                { event, keyCode ->
                    val callbackLua = callback.state()
                    callbackLua.push(callback)
                    callbackLua.push(event, Lua.Conversion.NONE)
                    callbackLua.push(keyCode, Lua.Conversion.FULL)
                    callbackLua.xpCall(
                        2,
                        1,
                        ConstantTrace("[ui keyUp \"${Input.Keys.toString(keyCode)}\"] registered in $registrationSite")
                    )
                    callbackLua.toBoolean(-1)
                }
            },
            keyDown = lua.getFieldFunction(1, "KeyDown")?.let { callback ->
                { event, keyCode ->
                    val callbackLua = callback.state()
                    callbackLua.push(callback)
                    callbackLua.push(event, Lua.Conversion.NONE)
                    callbackLua.push(keyCode, Lua.Conversion.FULL)
                    callbackLua.xpCall(
                        2,
                        1,
                        ConstantTrace("[ui keyDown \"${Input.Keys.toString(keyCode)}\"] registered in $registrationSite")
                    )
                    callbackLua.toBoolean(-1)
                }
            },
            keyTyped = lua.getFieldFunction(1, "KeyTyped")?.let { callback ->
                { event, character ->
                    val callbackLua = callback.state()
                    callbackLua.push(callback)
                    callbackLua.push(event, Lua.Conversion.NONE)
                    callbackLua.push(character, Lua.Conversion.FULL)
                    callbackLua.xpCall(
                        2,
                        1,
                        ConstantTrace("[ui keyTyped \"$character\"] registered in $registrationSite")
                    )
                    callbackLua.toBoolean(-1)
                }
            }
        )
        return 0
    }

    private fun luaSetFocus(lua: Lua): Int {
        val actor = if (lua.isUserdata(1)) lua.checkUserdata<Actor>(1) else null
        api.setFocus(actor)
        return 0
    }

    private fun luaGetFocus(lua: Lua): Int {
        val actor = api.getFocus()
        if (actor != null) {
            lua.push(actor, Lua.Conversion.NONE)
        } else {
            lua.pushNil()
        }
        return 1
    }

    private fun luaAddToRoot(lua: Lua): Int {
        val actors = mutableListOf<Actor>()
        if (lua.isUserdata(1) && lua.toUserdata<HudApi>(1) != null) {
            api.addToRoot(lua.checkUserdata<HudApi>(1))
        } else if (lua.isTable(1)) {
            actors += lua.toList(1)?.filterIsInstance<Actor>().orEmpty()
            api.addToRoot(actors)
        } else if (lua.isUserdata(1)) {
            actors += lua.checkUserdata(1, Actor::class)
            api.addToRoot(actors)
        }
        return 0
    }

    private fun luaLoadUI(lua: Lua): Int {
        val xmlFilePath = lua.checkString(1)
        if (lua.top >= 2) {
            lua.checkType(2, Lua.LuaType.TABLE)
        }

        val registrationSite = lua.getCallerInfo()
        val actions = mutableMapOf<String, (Any, Array<out Any>) -> Any?>()
        val i18nBundle = lua.getFieldString(2, "i18nBundle") ?: "system"
        val theme = lua.getFieldUserdata(2, "theme", ThemeApi::class)

        if (lua.isTable(2)) {
            lua.getField(2, "actions")
            if (lua.isTable(-1)) {
                lua.toTypedMap<String, LuaValue>(-1)?.forEach { (actionName, callback) ->
                    actions[actionName] = { widget, parameters ->
                        val callbackLua = callback.state()
                        callbackLua.push(callback, Lua.Conversion.NONE)
                        callbackLua.push(widget, Lua.Conversion.NONE)
                        for (parameter in parameters) {
                            callbackLua.push(parameter, Lua.Conversion.FULL)
                        }
                        callbackLua.xpCall(
                            parameters.size + 1,
                            1,
                            ConstantTrace("[ui action \"$actionName\"] registered in $registrationSite")
                        )
                        callbackLua.toAny(-1)
                    }
                }
            }
            lua.pop(1)
        }


        lua.push(api.loadUI(
            xmlFilePath = xmlFilePath,
            i18nBundle = i18nBundle,
            theme = theme,
            actions = actions
        ), Lua.Conversion.NONE)
        return 1
    }

    private fun luaLoadTheme(lua: Lua): Int {
        val atlas = if (lua.isUserdata(2)) lua.checkUserdata<TextureAtlas>(2) else null
        if (lua.isString(1)) {
            lua.push(api.loadTheme(lua.checkString(2), atlas), Lua.Conversion.NONE)
            return 1
        }
        val themeData = seleneJson.decodeFromJsonElement(
            ThemeDefinition.serializer(),
            (lua.toSerializedMap(1) ?: emptyMap()).toJsonElement()
        )
        lua.push(api.loadTheme(themeData, atlas), Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateTheme(lua: Lua): Int {
        lua.push(api.createTheme(), Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateContainer(lua: Lua): Int {
        val theme = lua.checkUserdata(1, ThemeApi::class)
        if (lua.top > 1) {
            lua.checkType(2, Lua.LuaType.TABLE)
        }

        val container = api.createContainer(
            theme = theme,
            child = lua.getFieldUserdata(2, "child", Actor::class),
            background = lua.getFieldString(2, "background"),
            width = lua.getFieldFloat(2, "width"),
            height = lua.getFieldFloat(2, "height")
        )
        lua.push(container, Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateLabel(lua: Lua): Int {
        val theme = lua.checkUserdata(1, ThemeApi::class)
        if (lua.top > 1) {
            lua.checkType(2, Lua.LuaType.TABLE)
        }

        lua.push(
            api.createLabel(
                theme = theme,
                style = lua.getFieldString(2, "style") ?: "default",
                text = lua.getFieldString(2, "text") ?: "",
                wrap = lua.getFieldBoolean(2, "wrap") ?: false
            ),
            Lua.Conversion.NONE
        )
        return 1
    }

    private fun luaCreateButtonStyle(lua: Lua): Int {
        val theme = lua.toUserdata<ThemeApi>(2)
        val styles = createButtonStyle(lua, 1, theme)
        for (style in styles) {
            lua.push(style, Lua.Conversion.NONE)
        }
        return styles.size
    }

    private fun luaCreateImageButtonStyle(lua: Lua): Int {
        val theme = lua.toUserdata<ThemeApi>(2)
        val styles = createImageButtonStyle(lua, 1, theme)
        for (style in styles) {
            lua.push(style, Lua.Conversion.NONE)
        }
        return styles.size
    }

    private fun luaCreateDragListener(lua: Lua): Int {
        lua.checkType(1, Lua.LuaType.TABLE)
        val registrationSite = lua.getCallerInfo()
        lua.push(
            api.createDragListener(
                onStart = lua.getFieldFunction(1, "onStart")?.let { callback ->
                    { draggable, actor, stageX, stageY ->
                        val callbackLua = callback.state()
                        callbackLua.push(callback)
                        callbackLua.push(draggable, Lua.Conversion.NONE)
                        callbackLua.push(actor, Lua.Conversion.NONE)
                        callbackLua.push(stageX)
                        callbackLua.push(stageY)
                        callbackLua.xpCall(
                            4,
                            1,
                            ConstantTrace("[drag listener] created in $registrationSite")
                        )
                        callbackLua.toBoolean(-1)
                    }
                },
                onDrag = lua.getFieldFunction(1, "onDrag")?.let { callback ->
                    { draggable, actor, stageX, stageY ->
                        val callbackLua = callback.state()
                        callbackLua.push(callback)
                        callbackLua.push(draggable, Lua.Conversion.NONE)
                        callbackLua.push(actor, Lua.Conversion.NONE)
                        callbackLua.push(stageX)
                        callbackLua.push(stageY)
                        callbackLua.xpCall(
                            4,
                            0,
                            ConstantTrace("[drag listener] created in $registrationSite")
                        )
                    }
                },
                onEnd = lua.getFieldFunction(1, "onEnd")?.let { callback ->
                    { draggable, actor, stageX, stageY ->
                        val callbackLua = callback.state()
                        callbackLua.push(callback)
                        callbackLua.push(draggable, Lua.Conversion.NONE)
                        callbackLua.push(actor, Lua.Conversion.NONE)
                        callbackLua.push(stageX)
                        callbackLua.push(stageY)
                        callbackLua.xpCall(
                            4,
                            1,
                            ConstantTrace("[drag listener] created in $registrationSite")
                        )
                        callbackLua.toBoolean(-1)
                    }
                }
            ),
            Lua.Conversion.NONE
        )
        return 1
    }

    fun createButtonStyle(
        lua: Lua,
        tableIndex: Int,
        theme: ThemeApi? = null
    ): List<Button.ButtonStyle> {
        lua.checkType(tableIndex, Lua.LuaType.TABLE)

        val up = lua.getFieldDrawable(tableIndex, "up", theme)
        val down = lua.getFieldDrawable(tableIndex, "down", theme)
        val checked = lua.getFieldDrawable(tableIndex, "checked", theme)
        val buttonStyle = Button.ButtonStyle(up, down, checked)
        lua.getFieldDrawable(tableIndex, "over", theme)?.let {
            buttonStyle.over = it
        }
        lua.getFieldDrawable(tableIndex, "focused", theme)?.let {
            buttonStyle.focused = it
        }
        lua.getFieldDrawable(tableIndex, "disabled", theme)?.let {
            buttonStyle.disabled = it
        }
        lua.getFieldDrawable(tableIndex, "checkedOver", theme)?.let {
            buttonStyle.checkedOver = it
        }
        lua.getFieldDrawable(tableIndex, "checkedDown", theme)?.let {
            buttonStyle.checkedDown = it
        }
        lua.getFieldDrawable(tableIndex, "checkedFocused", theme)?.let {
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
        theme: ThemeApi? = null
    ): List<Button.ButtonStyle> {
        lua.checkType(tableIndex, Lua.LuaType.TABLE)

        val up = lua.getFieldDrawable(tableIndex, "up", theme)
        val down = lua.getFieldDrawable(tableIndex, "down", theme)
        val checked = lua.getFieldDrawable(tableIndex, "checked", theme)
        val imageUp = lua.getFieldDrawable(tableIndex, "imageUp", theme)
        val imageDown = lua.getFieldDrawable(tableIndex, "imageDown", theme)
        val imageChecked = lua.getFieldDrawable(tableIndex, "imageChecked", theme)
        val imageButtonStyle = ImageButton.ImageButtonStyle(up, down, checked, imageUp, imageDown, imageChecked)
        val visImageButtonStyle = VisImageButton.VisImageButtonStyle(
            up, down, checked, imageUp, imageDown, imageChecked
        )

        lua.getFieldDrawable(tableIndex, "over", theme)?.let {
            imageButtonStyle.over = it
            visImageButtonStyle.over = it
        }

        lua.getFieldDrawable(tableIndex, "checkedOver", theme)?.let {
            imageButtonStyle.checkedOver = it
            visImageButtonStyle.checkedOver = it
        }

        lua.getFieldDrawable(tableIndex, "disabled", theme)?.let {
            imageButtonStyle.disabled = it
            visImageButtonStyle.disabled = it
        }

        lua.getFieldDrawable(tableIndex, "imageOver", theme)?.let {
            imageButtonStyle.imageOver = it
            visImageButtonStyle.imageOver = it
        }

        lua.getFieldDrawable(tableIndex, "imageCheckedOver", theme)?.let {
            imageButtonStyle.imageCheckedOver = it
            visImageButtonStyle.imageCheckedOver = it
        }

        lua.getFieldDrawable(tableIndex, "imageDisabled", theme)?.let {
            imageButtonStyle.imageDisabled = it
            visImageButtonStyle.imageDisabled = it
        }

        return listOf(imageButtonStyle, visImageButtonStyle)
    }

    fun Lua.getFieldDrawable(tableIndex: Int, fieldName: String, theme: ThemeApi?): Drawable? {
        return getField(tableIndex, fieldName) { type ->
            when (type) {
                Lua.LuaType.STRING -> api.skinResolvers.resolveDrawable(theme, toString(-1)!!)
                Lua.LuaType.USERDATA -> {
                    when (val value = toJavaObject(-1)) {
                        is Visual2D -> Visual2DDrawable(value)
                        is com.seleneworlds.client.rendering.drawable.Drawable -> DrawableDrawable(value)
                        else -> null
                    }
                }

                else -> null
            }
        }
    }

    val setup = LuaEventSink(ClientEvents.SetupUI.EVENT) { callback, trace ->
        ClientEvents.SetupUI { callback.runCoroutine(mainThreadDispatcher, trace) }
    }
}

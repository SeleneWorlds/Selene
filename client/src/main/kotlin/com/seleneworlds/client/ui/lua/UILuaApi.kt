package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.kotcrab.vis.ui.widget.VisImageButton
import com.seleneworlds.client.game.ClientEvents
import com.seleneworlds.client.ui.BundleUiDialogs
import com.seleneworlds.client.ui.HudApi
import com.seleneworlds.client.ui.ThemeApi
import com.seleneworlds.client.ui.ThemeDefinition
import com.seleneworlds.client.ui.UIApi
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.lua.LuaEventSink
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.*
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.toAny
import com.seleneworlds.common.script.ConstantTrace
import com.seleneworlds.common.script.ExposedApi
import com.seleneworlds.common.serialization.seleneJson
import com.seleneworlds.common.serialization.toJsonElement
import com.seleneworlds.common.threading.MainThreadDispatcher
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

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
        luaManager.defineMetatable(ScrollPane::class, ScrollPaneLuaMetatable.luaMeta)
        luaManager.defineMetatable(Container::class, ContainerLuaMetatable.luaMeta)
        luaManager.defineMetatable(Label::class, LabelLuaMetatable.luaMeta)
        luaManager.defineMetatable(TextField::class, TextFieldLuaMetatable.luaMeta)
        luaManager.defineMetatable(SelectBox::class, SelectBoxLuaMetatable.luaMeta)
        luaManager.defineMetatable(Button::class, ButtonLuaMetatable.luaMeta)
        luaManager.defineMetatable(TextButton::class, ButtonLuaMetatable.luaMeta)
        luaManager.defineMetatable(ImageTextButton::class, ButtonLuaMetatable.luaMeta)
        val dialogMetatable = DialogLuaMetatable(api).luaMeta
        luaManager.defineMetatable(Dialog::class, dialogMetatable)
        luaManager.defineMetatable(LuaDialog::class, dialogMetatable)
        luaManager.defineMetatable(CheckBox::class, CheckBoxLuaMetatable.luaMeta)
        luaManager.defineMetatable(Image::class, ImageLuaMetatable(api).luaMeta)
        luaManager.defineMetatable(ImageButton::class, ImageButtonLuaMetatable(api).luaMeta)
        luaManager.defineMetatable(VisImageButton::class, VisImageButtonLuaMetatable(api).luaMeta)
        luaManager.defineMetatable(ProgressBar::class, ProgressBarLuaMetatable.luaMeta)
        luaManager.defineMetatable(Slider::class, SliderLuaMetatable.luaMeta)
        luaManager.defineMetatable(ThemeApi::class, ThemeLuaApi(this).luaMeta)
        luaManager.defineMetatable(HudApi::class, HudLuaApi.luaMeta)
        luaManager.defineMetatable(TextField.TextFieldClickListener::class, TextFieldClickListenerLuaMetatable.luaMeta)
    }

    override fun register(table: LuaValue) {
        table.register("loadUI", ::loadUI)
        table.register("loadTheme", ::loadTheme)
        table.register("createTheme", ::createTheme)
        table.register("createContainer", ::createContainer)
        table.register("createLabel", ::createLabel)
        table.register("createDialog", ::createDialog)
        table.register("openDialog", ::openDialog)
        table.register("closeDialog", ::closeDialog)
        table.register("addToRoot", ::addToRoot)
        table.register("setFocus", ::setFocus)
        table.register("getFocus", ::getFocus)
        table.register("createImageButtonStyle", ::createImageButtonStyle)
        table.register("createButtonStyle", ::createButtonStyle)
        table.register("addInputProcessor", ::addInputProcessor)
        table.register("createDragListener", ::createDragListener)
        table.register("createAtlas", ::createAtlas)
        table.set("root", api.bundlesRoot)
        table.set("setup", setup)
    }

    private fun createDialog(lua: Lua): Int {
        lua.push(buildDialog(lua), Lua.Conversion.NONE)
        return 1
    }

    private fun openDialog(lua: Lua): Int {
        val dialog = if (lua.isUserdata(1) && lua.toUserdata<Dialog>(1) != null) {
            lua.checkUserdata<Dialog>(1)
        } else {
            buildDialog(lua)
        }
        api.openDialog(dialog)
        lua.push(dialog, Lua.Conversion.NONE)
        return 1
    }

    private fun closeDialog(lua: Lua): Int {
        api.closeDialog(lua.checkUserdata<Dialog>(1))
        return 0
    }

    private fun buildDialog(lua: Lua): LuaDialog {
        val (theme, configIndex) = if (lua.isUserdata(1) && lua.toUserdata<ThemeApi>(1) != null) {
            lua.checkUserdata<ThemeApi>(1) to 2
        } else {
            null to 1
        }
        if (lua.top >= configIndex) {
            lua.checkType(configIndex, Lua.LuaType.TABLE)
        }

        val dialog = LuaDialog(
            title = lua.getFieldString(configIndex, "title") ?: "",
            skin = theme?.skin ?: api.systemSkin
        )
        lua.getFieldString(configIndex, "content")?.let { content ->
            dialog.text(content)
        }

        if (lua.isTable(configIndex)) {
            lua.getField(configIndex, "buttons")
            val buttons = if (lua.isTable(-1)) lua.toAny(-1) else null
            lua.pop(1)

            (buttons as? List<*>)?.forEach { button ->
                when (button) {
                    is String -> dialog.addCallbackButton(
                        text = button,
                        callback = null,
                        trace = ConstantTrace("[dialog button \"$button\"] registered in ${lua.getCallerInfo()}")
                    )

                    is Map<*, *> -> {
                        val text = button["text"] as? String ?: return@forEach
                        val callback = button["callback"] as? LuaValue
                        dialog.addCallbackButton(
                            text = text,
                            callback = callback,
                            trace = ConstantTrace("[dialog button \"$text\"] registered in ${lua.getCallerInfo()}")
                        )
                    }

                    is List<*> -> {
                        val text = button.getOrNull(0) as? String ?: return@forEach
                        val callback = button.getOrNull(1) as? LuaValue
                        dialog.addCallbackButton(
                            text = text,
                            callback = callback,
                            trace = ConstantTrace("[dialog button \"$text\"] registered in ${lua.getCallerInfo()}")
                        )
                    }
                }
            }
        }
        BundleExecutionContext.currentBundle?.let { BundleUiDialogs.record(it, dialog) }
        return dialog
    }

    private fun createAtlas(lua: Lua): Int {
        val textures = lua.checkSerializedMap(1)
        lua.push(api.createAtlas(textures), Lua.Conversion.NONE)
        return 1
    }

    private fun addInputProcessor(lua: Lua): Int {
        lua.checkType(1, Lua.LuaType.TABLE)
        val registrationSite = lua.getCallerInfo()
        val bundle = BundleExecutionContext.currentBundle
        api.addInputProcessor(
            bundle = bundle,
            keyUp = lua.getFieldFunction(1, "KeyUp")?.let { callback ->
                { event, keyCode ->
                    runInputProcessorCallback(
                        callback = callback,
                        callbackName = "KeyUp",
                        trace = ConstantTrace("[ui keyUp \"${Input.Keys.toString(keyCode)}\"] registered in $registrationSite")
                    ) { callbackLua, callbackTrace ->
                        callbackLua.push(event, Lua.Conversion.NONE)
                        callbackLua.push(keyCode, Lua.Conversion.FULL)
                        callbackLua.xpCall(2, 1, callbackTrace)
                        callbackLua.toBoolean(-1)
                    }
                }
            },
            keyDown = lua.getFieldFunction(1, "KeyDown")?.let { callback ->
                { event, keyCode ->
                    runInputProcessorCallback(
                        callback = callback,
                        callbackName = "KeyDown",
                        trace = ConstantTrace("[ui keyDown \"${Input.Keys.toString(keyCode)}\"] registered in $registrationSite")
                    ) { callbackLua, callbackTrace ->
                        callbackLua.push(event, Lua.Conversion.NONE)
                        callbackLua.push(keyCode, Lua.Conversion.FULL)
                        callbackLua.xpCall(2, 1, callbackTrace)
                        callbackLua.toBoolean(-1)
                    }
                }
            },
            keyTyped = lua.getFieldFunction(1, "KeyTyped")?.let { callback ->
                { event, character ->
                    runInputProcessorCallback(
                        callback = callback,
                        callbackName = "KeyTyped",
                        trace = ConstantTrace("[ui keyTyped \"$character\"] registered in $registrationSite")
                    ) { callbackLua, callbackTrace ->
                        callbackLua.push(event, Lua.Conversion.NONE)
                        callbackLua.push(character, Lua.Conversion.FULL)
                        callbackLua.xpCall(2, 1, callbackTrace)
                        callbackLua.toBoolean(-1)
                    }
                }
            }
        )
        return 0
    }

    private fun runInputProcessorCallback(
        callback: LuaValue,
        callbackName: String,
        trace: ConstantTrace,
        block: (Lua, ConstantTrace) -> Boolean
    ): Boolean {
        val callbackLua = callback.state()
        callbackLua.push(callback)
        return try {
            block(callbackLua, trace)
        } catch (e: LuaException) {
            logger.error("Lua error in UI input processor callback {}", callbackName, e)
            false
        }
    }

    private fun setFocus(lua: Lua): Int {
        val actor = if (lua.isUserdata(1)) lua.checkUserdata<Actor>(1) else null
        api.setFocus(actor)
        return 0
    }

    private fun getFocus(lua: Lua): Int {
        val actor = api.getFocus()
        if (actor != null) {
            lua.push(actor, Lua.Conversion.NONE)
        } else {
            lua.pushNil()
        }
        return 1
    }

    private fun addToRoot(lua: Lua): Int {
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

    private fun loadUI(lua: Lua): Int {
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

    private fun loadTheme(lua: Lua): Int {
        val atlas = if (lua.isUserdata(2)) lua.checkUserdata<TextureAtlas>(2) else null
        if (lua.isString(1)) {
            lua.push(api.loadTheme(lua.checkString(1), atlas), Lua.Conversion.NONE)
            return 1
        }
        val themeData = seleneJson.decodeFromJsonElement(
            ThemeDefinition.serializer(),
            (lua.toSerializedMap(1) ?: emptyMap()).toJsonElement()
        )
        lua.push(api.loadTheme(themeData, atlas), Lua.Conversion.NONE)
        return 1
    }

    private fun createTheme(lua: Lua): Int {
        lua.push(api.createTheme(), Lua.Conversion.NONE)
        return 1
    }

    private fun createContainer(lua: Lua): Int {
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

    private fun createLabel(lua: Lua): Int {
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

    private fun createButtonStyle(lua: Lua): Int {
        val theme = lua.toUserdata<ThemeApi>(2)
        val styles = createButtonStyle(lua, 1, theme)
        for (style in styles) {
            lua.push(style, Lua.Conversion.NONE)
        }
        return styles.size
    }

    private fun createImageButtonStyle(lua: Lua): Int {
        val theme = lua.toUserdata<ThemeApi>(2)
        val styles = createImageButtonStyle(lua, 1, theme)
        for (style in styles) {
            lua.push(style, Lua.Conversion.NONE)
        }
        return styles.size
    }

    private fun createDragListener(lua: Lua): Int {
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

        val up = lua.getFieldDrawable(api, tableIndex, "up", theme)
        val down = lua.getFieldDrawable(api, tableIndex, "down", theme)
        val checked = lua.getFieldDrawable(api, tableIndex, "checked", theme)
        val buttonStyle = Button.ButtonStyle(up, down, checked)
        lua.getFieldDrawable(api, tableIndex, "over", theme)?.let {
            buttonStyle.over = it
        }
        lua.getFieldDrawable(api, tableIndex, "focused", theme)?.let {
            buttonStyle.focused = it
        }
        lua.getFieldDrawable(api, tableIndex, "disabled", theme)?.let {
            buttonStyle.disabled = it
        }
        lua.getFieldDrawable(api, tableIndex, "checkedOver", theme)?.let {
            buttonStyle.checkedOver = it
        }
        lua.getFieldDrawable(api, tableIndex, "checkedDown", theme)?.let {
            buttonStyle.checkedDown = it
        }
        lua.getFieldDrawable(api, tableIndex, "checkedFocused", theme)?.let {
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

        val up = lua.getFieldDrawable(api, tableIndex, "up", theme)
        val down = lua.getFieldDrawable(api, tableIndex, "down", theme)
        val checked = lua.getFieldDrawable(api, tableIndex, "checked", theme)
        val imageUp = lua.getFieldDrawable(api, tableIndex, "imageUp", theme)
        val imageDown = lua.getFieldDrawable(api, tableIndex, "imageDown", theme)
        val imageChecked = lua.getFieldDrawable(api, tableIndex, "imageChecked", theme)
        val imageButtonStyle = ImageButton.ImageButtonStyle(up, down, checked, imageUp, imageDown, imageChecked)
        val visImageButtonStyle = VisImageButton.VisImageButtonStyle(
            up, down, checked, imageUp, imageDown, imageChecked
        )

        lua.getFieldDrawable(api, tableIndex, "over", theme)?.let {
            imageButtonStyle.over = it
            visImageButtonStyle.over = it
        }

        lua.getFieldDrawable(api, tableIndex, "checkedOver", theme)?.let {
            imageButtonStyle.checkedOver = it
            visImageButtonStyle.checkedOver = it
        }

        lua.getFieldDrawable(api, tableIndex, "disabled", theme)?.let {
            imageButtonStyle.disabled = it
            visImageButtonStyle.disabled = it
        }

        lua.getFieldDrawable(api, tableIndex, "imageOver", theme)?.let {
            imageButtonStyle.imageOver = it
            visImageButtonStyle.imageOver = it
        }

        lua.getFieldDrawable(api, tableIndex, "imageCheckedOver", theme)?.let {
            imageButtonStyle.imageCheckedOver = it
            visImageButtonStyle.imageCheckedOver = it
        }

        lua.getFieldDrawable(api, tableIndex, "imageDisabled", theme)?.let {
            imageButtonStyle.imageDisabled = it
            visImageButtonStyle.imageDisabled = it
        }

        return listOf(imageButtonStyle, visImageButtonStyle)
    }

    val setup = LuaEventSink(ClientEvents.SetupUI.EVENT) { callback, trace ->
        ClientEvents.SetupUI { callback.runCoroutine(mainThreadDispatcher, trace) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UILuaApi::class.java)
    }
}

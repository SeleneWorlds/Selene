package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Container
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.kotcrab.vis.ui.widget.VisImageButton
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.client.rendering.visual2d.Visual2D
import com.seleneworlds.client.ui.UIApi
import com.seleneworlds.client.ui.drawable.DrawableDrawable
import com.seleneworlds.client.ui.drawable.Visual2DDrawable
import com.seleneworlds.common.script.ClosureTrace
import com.seleneworlds.common.script.ExposedApi
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkType
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.getField
import com.seleneworlds.common.lua.util.getFieldBoolean
import com.seleneworlds.common.lua.util.getFieldFloat
import com.seleneworlds.common.lua.util.getFieldFunction
import com.seleneworlds.common.lua.util.getFieldString
import com.seleneworlds.common.lua.util.getFieldUserdata
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.toAny
import com.seleneworlds.common.lua.util.toSerializedMap
import com.seleneworlds.common.lua.util.toTypedMap
import com.seleneworlds.common.lua.util.toUserdata
import com.seleneworlds.common.lua.util.xpCall

/**
 * Load, skin and manipulate UIs.
 */
class UILuaApi(
    override val api: UIApi
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
        luaManager.defineMetatable(Skin::class, SkinLuaMetatable(this).luaMeta)
        luaManager.defineMetatable(TextField.TextFieldClickListener::class, TextFieldClickListenerLuaMetatable.luaMeta)
    }

    override fun register(table: LuaValue) {
        table.register("LoadUI", ::luaLoadUI)
        table.register("LoadSkin", ::luaLoadSkin)
        table.register("CreateSkin", ::luaCreateSkin)
        table.register("CreateContainer", ::luaCreateContainer)
        table.register("CreateLabel", ::luaCreateLabel)
        table.register("AddToRoot", ::luaAddToRoot)
        table.register("SetFocus", ::luaSetFocus)
        table.register("GetFocus", ::luaGetFocus)
        table.register("CreateImageButtonStyle", ::luaCreateImageButtonStyle)
        table.register("CreateButtonStyle", ::luaCreateButtonStyle)
        table.register("AddInputProcessor", ::luaAddInputProcessor)
        table.register("CreateDragListener", ::luaCreateDragListener)
        table.set("Root", api.bundlesRoot)
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
                        ClosureTrace { "[ui keyUp \"${Input.Keys.toString(keyCode)}\"] registered in $registrationSite" })
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
                        ClosureTrace { "[ui keyDown \"${Input.Keys.toString(keyCode)}\"] registered in $registrationSite" })
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
                        ClosureTrace { "[ui keyTyped \"$character\"] registered in $registrationSite" })
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
        if (lua.isTable(1)) {
            lua.toSerializedMap(1)?.values?.forEach { actor ->
                if (actor is Actor) {
                    actors += actor
                }
            }
        } else if (lua.isUserdata(1)) {
            actors += lua.checkUserdata(1, Actor::class)
        }
        api.addToRoot(actors)
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
        val skin = lua.getFieldUserdata(2, "skin", Skin::class)

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
                            ClosureTrace { "[ui action \"$actionName\"] registered in $registrationSite" })
                        callbackLua.toAny(-1)
                    }
                }
            }
            lua.pop(1)
        }

        return try {
            val (actors, actorsByName) = api.loadUI(
                xmlFilePath = xmlFilePath,
                i18nBundle = i18nBundle,
                skin = skin,
                actions = actions
            )
            lua.push(actors, Lua.Conversion.FULL)
            lua.push(actorsByName, Lua.Conversion.FULL)
            2
        } catch (e: Exception) {
            lua.error(e)
        }
    }

    private fun luaLoadSkin(lua: Lua): Int {
        return try {
            lua.push(api.loadSkin(lua.checkString(1)), Lua.Conversion.NONE)
            1
        } catch (e: Exception) {
            lua.error(e)
        }
    }

    private fun luaCreateSkin(lua: Lua): Int {
        lua.push(api.createSkin(), Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateContainer(lua: Lua): Int {
        val skin = lua.checkUserdata(1, Skin::class)
        if (lua.top > 1) {
            lua.checkType(2, Lua.LuaType.TABLE)
        }

        val container = api.createContainer(
            skin = skin,
            child = lua.getFieldUserdata(2, "child", Actor::class),
            background = lua.getFieldString(2, "background"),
            width = lua.getFieldFloat(2, "width"),
            height = lua.getFieldFloat(2, "height")
        )
        lua.push(container, Lua.Conversion.NONE)
        return 1
    }

    private fun luaCreateLabel(lua: Lua): Int {
        val skin = lua.checkUserdata(1, Skin::class)
        if (lua.top > 1) {
            lua.checkType(2, Lua.LuaType.TABLE)
        }

        lua.push(
            api.createLabel(
                skin = skin,
                style = lua.getFieldString(2, "style") ?: "default",
                text = lua.getFieldString(2, "text") ?: "",
                wrap = lua.getFieldBoolean(2, "wrap") ?: false
            ),
            Lua.Conversion.NONE
        )
        return 1
    }

    private fun luaCreateButtonStyle(lua: Lua): Int {
        val skin = lua.toUserdata<Skin>(2)
        val styles = createButtonStyle(lua, 1, skin)
        for (style in styles) {
            lua.push(style, Lua.Conversion.NONE)
        }
        return styles.size
    }

    private fun luaCreateImageButtonStyle(lua: Lua): Int {
        val skin = lua.toUserdata<Skin>(2)
        val styles = createImageButtonStyle(lua, 1, skin)
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
                        callbackLua.xpCall(4, 1, ClosureTrace { "[drag listener] created in $registrationSite" })
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
                        callbackLua.xpCall(4, 0, ClosureTrace { "[drag listener] created in $registrationSite" })
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
                        callbackLua.xpCall(4, 1, ClosureTrace { "[drag listener] created in $registrationSite" })
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
        skin: Skin? = null
    ): List<Button.ButtonStyle> {
        lua.checkType(tableIndex, Lua.LuaType.TABLE)

        val up = lua.getFieldDrawable(tableIndex, "up", skin)
        val down = lua.getFieldDrawable(tableIndex, "down", skin)
        val checked = lua.getFieldDrawable(tableIndex, "checked", skin)
        val buttonStyle = Button.ButtonStyle(up, down, checked)
        lua.getFieldDrawable(tableIndex, "over", skin)?.let {
            buttonStyle.over = it
        }
        lua.getFieldDrawable(tableIndex, "focused", skin)?.let {
            buttonStyle.focused = it
        }
        lua.getFieldDrawable(tableIndex, "disabled", skin)?.let {
            buttonStyle.disabled = it
        }
        lua.getFieldDrawable(tableIndex, "checkedOver", skin)?.let {
            buttonStyle.checkedOver = it
        }
        lua.getFieldDrawable(tableIndex, "checkedDown", skin)?.let {
            buttonStyle.checkedDown = it
        }
        lua.getFieldDrawable(tableIndex, "checkedFocused", skin)?.let {
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
        skin: Skin? = null
    ): List<Button.ButtonStyle> {
        lua.checkType(tableIndex, Lua.LuaType.TABLE)

        val up = lua.getFieldDrawable(tableIndex, "up", skin)
        val down = lua.getFieldDrawable(tableIndex, "down", skin)
        val checked = lua.getFieldDrawable(tableIndex, "checked", skin)
        val imageUp = lua.getFieldDrawable(tableIndex, "imageUp", skin)
        val imageDown = lua.getFieldDrawable(tableIndex, "imageDown", skin)
        val imageChecked = lua.getFieldDrawable(tableIndex, "imageChecked", skin)
        val imageButtonStyle = ImageButton.ImageButtonStyle(up, down, checked, imageUp, imageDown, imageChecked)
        val visImageButtonStyle = VisImageButton.VisImageButtonStyle(
            up, down, checked, imageUp, imageDown, imageChecked
        )

        lua.getFieldDrawable(tableIndex, "over", skin)?.let {
            imageButtonStyle.over = it
            visImageButtonStyle.over = it
        }

        lua.getFieldDrawable(tableIndex, "checkedOver", skin)?.let {
            imageButtonStyle.checkedOver = it
            visImageButtonStyle.checkedOver = it
        }

        lua.getFieldDrawable(tableIndex, "disabled", skin)?.let {
            imageButtonStyle.disabled = it
            visImageButtonStyle.disabled = it
        }

        lua.getFieldDrawable(tableIndex, "imageOver", skin)?.let {
            imageButtonStyle.imageOver = it
            visImageButtonStyle.imageOver = it
        }

        lua.getFieldDrawable(tableIndex, "imageCheckedOver", skin)?.let {
            imageButtonStyle.imageCheckedOver = it
            visImageButtonStyle.imageCheckedOver = it
        }

        lua.getFieldDrawable(tableIndex, "imageDisabled", skin)?.let {
            imageButtonStyle.imageDisabled = it
            visImageButtonStyle.imageDisabled = it
        }

        return listOf(imageButtonStyle, visImageButtonStyle)
    }

    fun Lua.getFieldDrawable(tableIndex: Int, fieldName: String, skin: Skin?): Drawable? {
        return getField(tableIndex, fieldName) { type ->
            when (type) {
                Lua.LuaType.STRING -> api.skinResolvers.resolveDrawable(skin, toString(-1)!!)
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
}

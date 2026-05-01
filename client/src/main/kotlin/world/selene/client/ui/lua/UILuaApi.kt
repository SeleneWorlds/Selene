package world.selene.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkType
import world.selene.common.lua.util.checkUserdata
import world.selene.common.lua.util.getCallerInfo
import world.selene.common.lua.util.getField
import world.selene.common.lua.util.getFieldBoolean
import world.selene.common.lua.util.getFieldFloat
import world.selene.common.lua.util.getFieldFunction
import world.selene.common.lua.util.getFieldString
import world.selene.common.lua.util.getFieldUserdata
import world.selene.common.lua.util.register
import world.selene.common.lua.util.toAnyMap
import world.selene.common.lua.util.toTypedMap
import world.selene.common.lua.util.toUserdata

/**
 * Load, skin and manipulate UIs.
 */
class UILuaApi(
    private val api: UIApi
) : LuaModule {
    override val name = "selene.ui.lml"

    override fun initialize(luaManager: LuaManager) {
        api.initialize(luaManager)
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
        api.addInputProcessor(
            keyUp = lua.getFieldFunction(1, "KeyUp"),
            keyDown = lua.getFieldFunction(1, "KeyDown"),
            keyTyped = lua.getFieldFunction(1, "KeyTyped"),
            registrationSite = lua.getCallerInfo()
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
            lua.toAnyMap(1)?.values?.forEach { actor ->
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

        val actions = mutableMapOf<String, LuaValue>()
        val i18nBundle = lua.getFieldString(2, "i18nBundle") ?: "system"
        val skin = lua.getFieldUserdata(2, "skin", Skin::class)

        if (lua.isTable(2)) {
            lua.getField(2, "actions")
            if (lua.isTable(-1)) {
                lua.toTypedMap<String, LuaValue>(-1)?.forEach { (actionName, actionFunction) ->
                    actions[actionName] = actionFunction
                }
            }
            lua.pop(1)
        }

        return try {
            val (actors, actorsByName) = api.loadUI(
                xmlFilePath = xmlFilePath,
                i18nBundle = i18nBundle,
                skin = skin,
                actions = actions,
                registrationSite = lua.getCallerInfo()
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
        val styles = api.luaSkinUtils.createButtonStyle(lua, 1, skin)
        for (style in styles) {
            lua.push(style, Lua.Conversion.NONE)
        }
        return styles.size
    }

    private fun luaCreateImageButtonStyle(lua: Lua): Int {
        val skin = lua.toUserdata<Skin>(2)
        val styles = api.luaSkinUtils.createImageButtonStyle(lua, 1, skin)
        for (style in styles) {
            lua.push(style, Lua.Conversion.NONE)
        }
        return styles.size
    }

    private fun luaCreateDragListener(lua: Lua): Int {
        lua.checkType(1, Lua.LuaType.TABLE)
        lua.push(
            api.createDragListener(
                onStart = lua.getFieldFunction(1, "onStart"),
                onDrag = lua.getFieldFunction(1, "onDrag"),
                onEnd = lua.getFieldFunction(1, "onEnd"),
                registrationSite = lua.getCallerInfo()
            ),
            Lua.Conversion.NONE
        )
        return 1
    }
}

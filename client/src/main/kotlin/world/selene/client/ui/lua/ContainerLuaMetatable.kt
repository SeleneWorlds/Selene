package world.selene.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Container
import party.iroiro.luajava.Lua
import world.selene.common.lua.util.checkFloat
import world.selene.common.lua.util.checkUserdata

@Suppress("SameReturnValue")
object ContainerLuaMetatable {
    val luaMeta = GroupLuaMetatable.luaMeta.extend(Container::class) {
        setter(::luaSetMinWidth)
        setter(::luaSetMinHeight)
        setter(::luaSetWidth)
        setter(::luaSetHeight)
        setter(::luaSetMaxWidth)
        setter(::luaSetMaxHeight)
        callable(::luaAddChild)
    }

    private fun luaAddChild(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val child = it.checkUserdata(2, Actor::class)
        container.actor = child
        return 0
    }

    private fun luaSetWidth(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val minWidth = it.checkFloat(3)
        container.width(minWidth)
        return 0
    }

    private fun luaSetHeight(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val minHeight = it.checkFloat(3)
        container.height(minHeight)
        return 0
    }

    private fun luaSetMinWidth(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val minWidth = it.checkFloat(3)
        container.minWidth(minWidth)
        return 0
    }

    private fun luaSetMinHeight(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val minHeight = it.checkFloat(3)
        container.minHeight(minHeight)
        return 0
    }

    private fun luaSetMaxWidth(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val maxWidth = it.checkFloat(3)
        container.maxWidth(maxWidth)
        return 0
    }

    private fun luaSetMaxHeight(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val maxHeight = it.checkFloat(3)
        container.maxHeight(maxHeight)
        return 0
    }
}
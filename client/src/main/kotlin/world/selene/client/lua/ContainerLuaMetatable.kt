package world.selene.client.lua

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Container
import party.iroiro.luajava.Lua
import world.selene.common.lua.checkFloat
import world.selene.common.lua.checkUserdata

object ContainerLuaMetatable {
    val luaMeta = GroupLuaMetatable.luaMeta.extend(Container::class) {
        callable(::luaAddChild)
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
    }

    private fun luaAddChild(it: Lua): Int {
        val actor = it.checkUserdata<Container<Actor>>(1)
        val child = it.checkUserdata(2, Actor::class)
        actor.actor = child
        return 0
    }
}
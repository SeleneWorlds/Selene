package world.selene.client.lua

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import party.iroiro.luajava.Lua
import world.selene.common.lua.checkUserdata

@Suppress("SameReturnValue")
object GroupLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(Group::class) {
        callable(::luaAddChild)
        callable(::luaAddChildBefore)
    }

    private fun luaAddChild(lua: Lua): Int {
        val actor = lua.checkUserdata<Group>(1)
        val child = lua.checkUserdata(2, Actor::class)
        actor.addActor(child)
        return 0
    }

    private fun luaAddChildBefore(lua: Lua): Int {
        val actor = lua.checkUserdata<Group>(1)
        val before = lua.checkUserdata<Actor>(2)
        val child = lua.checkUserdata<Actor>(3)
        actor.addActorBefore(before, child)
        return 0
    }
}
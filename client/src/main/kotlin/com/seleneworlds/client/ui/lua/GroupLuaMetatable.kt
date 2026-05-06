package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.util.checkUserdata

object GroupLuaMetatable {
    val luaMeta = ActorLuaMetatable.luaMeta.extend(Group::class) {
        callable(::addChild)
        callable(::addChildBefore)
    }

    private fun addChild(lua: Lua): Int {
        val actor = lua.checkUserdata<Group>(1)
        val child = lua.checkUserdata(2, Actor::class)
        actor.addActor(child)
        return 0
    }

    private fun addChildBefore(lua: Lua): Int {
        val actor = lua.checkUserdata<Group>(1)
        val before = lua.checkUserdata<Actor>(2)
        val child = lua.checkUserdata<Actor>(3)
        actor.addActorBefore(before, child)
        return 0
    }
}
package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Container
import party.iroiro.luajava.Lua
import com.seleneworlds.common.lua.util.checkFloat
import com.seleneworlds.common.lua.util.checkUserdata

object ContainerLuaMetatable {
    val luaMeta = GroupLuaMetatable.luaMeta.extend(Container::class) {
        callable(::setMinWidth)
        callable(::setMinHeight)
        callable(::setWidth)
        callable(::setHeight)
        callable(::setMaxWidth)
        callable(::setMaxHeight)
        callable(::addChild)
    }

    private fun addChild(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val child = it.checkUserdata(2, Actor::class)
        container.actor = child
        return 0
    }

    private fun setWidth(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val minWidth = it.checkFloat(2)
        container.width(minWidth)
        return 0
    }

    private fun setHeight(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val minHeight = it.checkFloat(2)
        container.height(minHeight)
        return 0
    }

    private fun setMinWidth(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val minWidth = it.checkFloat(2)
        container.minWidth(minWidth)
        return 0
    }

    private fun setMinHeight(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val minHeight = it.checkFloat(2)
        container.minHeight(minHeight)
        return 0
    }

    private fun setMaxWidth(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val maxWidth = it.checkFloat(2)
        container.maxWidth(maxWidth)
        return 0
    }

    private fun setMaxHeight(it: Lua): Int {
        val container = it.checkUserdata<Container<Actor>>(1)
        val maxHeight = it.checkFloat(2)
        container.maxHeight(maxHeight)
        return 0
    }
}

package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.seleneworlds.common.lua.util.checkBoolean
import com.seleneworlds.common.lua.util.checkFloat
import com.seleneworlds.common.lua.util.checkUserdata
import party.iroiro.luajava.Lua

object ScrollPaneLuaMetatable {
    val luaMeta = GroupLuaMetatable.luaMeta.extend(ScrollPane::class) {
        callable(::setScrollPosition)
        callable(::setScrollX)
        callable(::setScrollY)
        callable(::getScrollX)
        callable(::getScrollY)
        callable(::scrollTo)
        callable(::scrollToActor)
        callable(::setOverscroll)
        callable(::setupOverscroll)
        callable(::setFadeScrollBars)
    }

    /**
     * Sets the current scroll position in pixels.
     *
     * ```signatures
     * setScrollPosition(x: number, y: number)
     * ```
     */
    private fun setScrollPosition(lua: Lua): Int {
        val scrollPane = lua.checkUserdata<ScrollPane>(1)
        scrollPane.setScrollX(lua.checkFloat(2))
        scrollPane.setScrollY(lua.checkFloat(3))
        scrollPane.updateVisualScroll()
        return 0
    }

    /**
     * ```property
     * ScrollX: number
     * ```
     */
    private fun setScrollX(lua: Lua): Int {
        val scrollPane = lua.checkUserdata<ScrollPane>(1)
        scrollPane.setScrollX(lua.checkFloat(2))
        scrollPane.updateVisualScroll()
        return 0
    }

    /**
     * ```property
     * ScrollY: number
     * ```
     */
    private fun setScrollY(lua: Lua): Int {
        val scrollPane = lua.checkUserdata<ScrollPane>(1)
        scrollPane.setScrollY(lua.checkFloat(2))
        scrollPane.updateVisualScroll()
        return 0
    }

    /**
     * ```property
     * ScrollX: number
     * ```
     */
    private fun getScrollX(lua: Lua): Int {
        val scrollPane = lua.checkUserdata<ScrollPane>(1)
        lua.push(scrollPane.scrollX)
        return 1
    }

    /**
     * ```property
     * ScrollY: number
     * ```
     */
    private fun getScrollY(lua: Lua): Int {
        val scrollPane = lua.checkUserdata<ScrollPane>(1)
        lua.push(scrollPane.scrollY)
        return 1
    }

    /**
     * Scrolls the specified rectangle into view.
     *
     * ```signatures
     * scrollTo(x: number, y: number, width: number, height: number)
     * ```
     */
    private fun scrollTo(lua: Lua): Int {
        val scrollPane = lua.checkUserdata<ScrollPane>(1)
        scrollPane.scrollTo(
            lua.checkFloat(2),
            lua.checkFloat(3),
            lua.checkFloat(4),
            lua.checkFloat(5)
        )
        scrollPane.updateVisualScroll()
        return 0
    }

    /**
     * Scrolls to make an actor inside this scroll pane visible.
     *
     * ```signatures
     * scrollToActor(actor: Actor)
     * ```
     */
    private fun scrollToActor(lua: Lua): Int {
        val scrollPane = lua.checkUserdata<ScrollPane>(1)
        val actor = lua.checkUserdata<Actor>(2)
        val contentActor = scrollPane.actor
        val position = when {
            actor == contentActor -> Vector2.Zero
            contentActor is Group -> actor.localToAscendantCoordinates(contentActor, Vector2.Zero.cpy())
            else -> Vector2(actor.x, actor.y)
        }
        scrollPane.scrollTo(position.x, position.y, actor.width, actor.height)
        scrollPane.updateVisualScroll()
        return 0
    }

    /**
     * Enables or disables overscroll for the horizontal and vertical axes.
     *
     * ```signatures
     * setOverscroll(enabled: boolean)
     * setOverscroll(horizontal: boolean, vertical: boolean)
     * ```
     */
    private fun setOverscroll(lua: Lua): Int {
        val scrollPane = lua.checkUserdata<ScrollPane>(1)
        val horizontal = lua.checkBoolean(2)
        val vertical = if (lua.isBoolean(3)) lua.checkBoolean(3) else horizontal
        scrollPane.setOverscroll(horizontal, vertical)
        return 0
    }

    /**
     * Configures overscroll distance and speed.
     *
     * ```signatures
     * setupOverscroll(distance: number, speedMin: number, speedMax: number)
     * ```
     */
    private fun setupOverscroll(lua: Lua): Int {
        val scrollPane = lua.checkUserdata<ScrollPane>(1)
        scrollPane.setupOverscroll(lua.checkFloat(2), lua.checkFloat(3), lua.checkFloat(4))
        return 0
    }

    /**
     * Enables or disables fade scrollbars.
     *
     * ```signatures
     * setFadeScrollBars(enabled: boolean)
     * ```
     */
    private fun setFadeScrollBars(lua: Lua): Int {
        val scrollPane = lua.checkUserdata<ScrollPane>(1)
        scrollPane.setFadeScrollBars(lua.checkBoolean(2))
        return 0
    }
}

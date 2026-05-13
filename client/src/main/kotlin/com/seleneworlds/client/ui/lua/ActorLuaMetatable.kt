package com.seleneworlds.client.ui.lua

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.kotcrab.vis.ui.widget.LinkLabel
import com.kotcrab.vis.ui.widget.VisTextField
import party.iroiro.luajava.Lua
import com.seleneworlds.client.ui.ThemeApi
import com.seleneworlds.common.lua.LuaMappedMetatable
import com.seleneworlds.common.lua.util.checkFloat
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.checkUserdata

object ActorLuaMetatable {
    val luaMeta = LuaMappedMetatable(Actor::class) {
        callable(::getStage)
        callable(::getName)
        callable(::getParent)
        callable(::isVisible)
        callable(::setVisible)
        callable(::getWidth)
        callable(::getHeight)
        callable(::setWidth)
        callable(::setHeight)
        callable(::getMinWidth)
        callable(::getMinHeight)
        callable(::getPreferredWidth)
        callable(::getPreferredHeight)
        callable(::getMaxWidth)
        callable(::getMaxHeight)
        callable(::invalidate)
        callable(::setStyle)
        callable(::focus)
    }

    /**
     * Stage this actor is attached to, or `nil` if it is not attached to any stage.
     *
     * ```property
     * Stage: Stage|nil
     * ```
     */
    private fun getStage(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        lua.push(actor.stage, Lua.Conversion.NONE)
        return 1
    }

    /**
     * Name of this actor, if set ("id" in LML).
     *
     * ```property
     * Name: string|nil
     * ```
     */
    private fun getName(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        actor.name?.let(lua::push) ?: lua.pushNil()
        return 1
    }

    /**
     * Parent actor or `nil` if it has no parent.
     *
     * ```property
     * Parent: Actor|nil
     * ```
     */
    private fun getParent(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        lua.push(actor.parent, Lua.Conversion.NONE)
        return 1
    }

    /**
     * Whether this actor is currently visible.
     *
     * ```property
     * Visible: boolean
     * ```
     */
    private fun isVisible(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        lua.push(actor.isVisible)
        return 1
    }

    /**
     * ```property
     * Visible: boolean
     * ```
     */
    private fun setVisible(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        actor.isVisible = lua.toBoolean(2)
        return 0
    }

    /**
     * Effective width of this actor.
     *
     * ```property
     * Width: number
     * ```
     */
    private fun getWidth(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        lua.push(actor.width)
        return 1
    }

    /**
     * Effective height of this actor.
     *
     * ```property
     * Height: number
     * ```
     */
    private fun getHeight(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        lua.push(actor.height)
        return 1
    }

    /**
     * ```property
     * Width: number
     * ```
     */
    private fun setWidth(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        val width = lua.checkFloat(2)
        actor.width = width
        return 0
    }

    /**
     * ```property
     * Height: number
     * ```
     */
    private fun setHeight(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        val height = lua.checkFloat(2)
        actor.height = height
        return 0
    }

    /**
     * Invalidates the actor's layout if it implements Layout interface.
     * Forces the actor to recalculate its layout on the next frame.
     *
     * ```signatures
     * Invalidate()
     * ```
     */
    private fun invalidate(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        if (actor is Layout) actor.invalidate()
        return 0
    }

    /**
     * Sets the style of the actor using a skin and style name.
     *
     * ```signatures
     * SetStyle(theme: ThemeApi, styleName: string)
     * ```
     */
    private fun setStyle(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        val theme = lua.checkUserdata<ThemeApi>(2)
        val style = lua.checkString(3)
        when (actor) {
            is VisTextField -> {
                actor.style = theme.skin.get(style, VisTextField.VisTextFieldStyle::class.java)
            }

            is LinkLabel -> {
                actor.style = theme.skin.get(style, LinkLabel.LinkLabelStyle::class.java)
            }

            is Label -> {
                actor.style = theme.skin.get(style, Label.LabelStyle::class.java)
            }

            is SelectBox<*> -> {
                actor.style = theme.skin.get(style, SelectBox.SelectBoxStyle::class.java)
            }

            is Slider -> {
                actor.style = theme.skin.get(style, Slider.SliderStyle::class.java)
            }

            is ProgressBar -> {
                actor.style = theme.skin.get(style, ProgressBar.ProgressBarStyle::class.java)
            }

            is TextField -> {
                actor.style = theme.skin.get(style, TextField.TextFieldStyle::class.java)
            }

            is List<*> -> {
                actor.style = theme.skin.get(style, List.ListStyle::class.java)
            }

            is Touchpad -> {
                actor.style = theme.skin.get(style, Touchpad.TouchpadStyle::class.java)
            }
        }
        return 0
    }

    /**
     * Grants keyboard focus to this actor.
     *
     * ```signatures
     * Focus()
     * ```
     */
    private fun focus(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        actor.stage.keyboardFocus = actor
        return 0
    }

    /**
     * Minimum width of the actor, if applicable.
     *
     * ```property
     * MinWidth: number
     * ```
     */
    private fun getMinWidth(it: Lua): Int {
        val actor = it.checkUserdata<Actor>(1)
        it.push((actor as? Layout)?.minWidth ?: 0f)
        return 1
    }

    /**
     * Minimum height of the actor, if applicable.
     *
     * ```property
     * MinHeight: number
     * ```
     */
    private fun getMinHeight(it: Lua): Int {
        val actor = it.checkUserdata<Actor>(1)
        it.push((actor as? Layout)?.minHeight ?: 0f)
        return 1
    }

    /**
     * Preferred width of the actor, if applicable.
     *
     * ```property
     * PreferredWidth: number
     * ```
     */
    private fun getPreferredWidth(it: Lua): Int {
        val actor = it.checkUserdata<Actor>(1)
        it.push((actor as? Layout)?.prefWidth ?: 0f)
        return 1
    }

    /**
     * Preferred height of the actor, if applicable.
     *
     * ```property
     * PreferredHeight: number
     * ```
     */
    private fun getPreferredHeight(it: Lua): Int {
        val actor = it.checkUserdata<Actor>(1)
        it.push((actor as? Layout)?.prefHeight ?: 0f)
        return 1
    }

    /**
     * Maximum width of the actor, if applicable.
     *
     * ```property
     * MaxWidth: number
     * ```
     */
    private fun getMaxWidth(it: Lua): Int {
        val actor = it.checkUserdata<Actor>(1)
        it.push((actor as? Layout)?.maxWidth ?: 0f)
        return 1
    }

    /**
     * Maximum height of the actor, if applicable.
     *
     * ```property
     * MaxHeight: number
     * ```
     */
    private fun getMaxHeight(it: Lua): Int {
        val actor = it.checkUserdata<Actor>(1)
        it.push((actor as? Layout)?.maxHeight ?: 0f)
        return 1
    }
}

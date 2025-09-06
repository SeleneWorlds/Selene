package world.selene.client.lua

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.Layout
import com.kotcrab.vis.ui.widget.LinkLabel
import com.kotcrab.vis.ui.widget.VisTextField
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.checkFloat
import world.selene.common.lua.checkString
import world.selene.common.lua.checkUserdata

object ActorLuaMetatable {
    val luaMeta = LuaMappedMetatable(Actor::class) {
        getter(::luaGetName)
        getter(::luaGetParent)
        getter(::luaGetWidth)
        getter(::luaGetHeight)
        setter(::luaSetWidth)
        setter(::luaSetHeight)
        getter(::luaGetMinWidth)
        getter(::luaGetMinHeight)
        getter(::luaGetPreferredWidth)
        getter(::luaGetPreferredHeight)
        getter(::luaGetMaxWidth)
        getter(::luaGetMaxHeight)
        callable(::luaInvalidate)
        callable(::luaSetStyle)
        callable(::luaFocus)
    }

    /**
     * Name of this actor, if set ("id" in LML).
     *
     * ```property
     * Name: string|nil
     * ```
     */
    private fun luaGetName(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        lua.push(actor.name)
        return 1
    }

    /**
     * Parent actor or `nil` if it has no parent.
     *
     * ```property
     * Parent: Actor|nil
     * ```
     */
    private fun luaGetParent(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        lua.push(actor.parent, Lua.Conversion.NONE)
        return 1
    }

    /**
     * Effective width of this actor.
     *
     * ```property
     * Width: number
     * ```
     */
    private fun luaGetWidth(lua: Lua): Int {
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
    private fun luaGetHeight(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        lua.push(actor.height)
        return 1
    }

    /**
     * ```property
     * Width: number
     * ```
     */
    private fun luaSetWidth(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        val width = lua.checkFloat(3)
        actor.width = width
        return 0
    }

    /**
     * ```property
     * Height: number
     * ```
     */
    private fun luaSetHeight(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        val height = lua.checkFloat(3)
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
    private fun luaInvalidate(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        if (actor is Layout) actor.invalidate()
        return 0
    }

    /**
     * Sets the style of the actor using a skin and style name.
     *
     * ```signatures
     * SetStyle(skin: Skin, styleName: string)
     * ```
     */
    private fun luaSetStyle(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        val skin = lua.checkUserdata<Skin>(2)
        val style = lua.checkString(3)
        when (actor) {
            is VisTextField -> {
                actor.style = skin.get(style, VisTextField.VisTextFieldStyle::class.java)
            }

            is LinkLabel -> {
                actor.style = skin.get(style, LinkLabel.LinkLabelStyle::class.java)
            }

            is Label -> {
                actor.style = skin.get(style, Label.LabelStyle::class.java)
            }

            is SelectBox<*> -> {
                actor.style = skin.get(style, SelectBox.SelectBoxStyle::class.java)
            }

            is Slider -> {
                actor.style = skin.get(style, Slider.SliderStyle::class.java)
            }

            is ProgressBar -> {
                actor.style = skin.get(style, ProgressBar.ProgressBarStyle::class.java)
            }

            is TextField -> {
                actor.style = skin.get(style, TextField.TextFieldStyle::class.java)
            }

            is com.badlogic.gdx.scenes.scene2d.ui.List<*> -> {
                actor.style = skin.get(style, com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle::class.java)
            }

            is Touchpad -> {
                actor.style = skin.get(style, Touchpad.TouchpadStyle::class.java)
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
    private fun luaFocus(lua: Lua): Int {
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
    private fun luaGetMinWidth(it: Lua): Int {
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
    private fun luaGetMinHeight(it: Lua): Int {
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
    private fun luaGetPreferredWidth(it: Lua): Int {
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
    private fun luaGetPreferredHeight(it: Lua): Int {
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
    private fun luaGetMaxWidth(it: Lua): Int {
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
    private fun luaGetMaxHeight(it: Lua): Int {
        val actor = it.checkUserdata<Actor>(1)
        it.push((actor as? Layout)?.maxHeight ?: 0f)
        return 1
    }
}
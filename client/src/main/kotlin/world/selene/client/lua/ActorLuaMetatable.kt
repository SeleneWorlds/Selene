package world.selene.client.lua

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Slider
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
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

    private fun luaGetName(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        lua.push(actor.name)
        return 1
    }

    private fun luaGetParent(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        lua.push(actor.parent, Lua.Conversion.NONE)
        return 1
    }

    private fun luaGetWidth(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        lua.push(actor.width)
        return 1
    }

    private fun luaGetHeight(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        lua.push(actor.height)
        return 1
    }

    private fun luaSetWidth(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        val width = lua.checkFloat(3)
        actor.width = width
        return 0
    }

    private fun luaSetHeight(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        val height = lua.checkFloat(3)
        actor.height = height
        return 0
    }

    private fun luaInvalidate(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        if (actor is Layout) actor.invalidate()
        return 0
    }

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

    private fun luaFocus(lua: Lua): Int {
        val actor = lua.checkUserdata<Actor>(1)
        actor.stage.keyboardFocus = actor
        return 0
    }

    private fun luaGetMinWidth(it: Lua): Int {
        val actor = it.checkUserdata<Actor>(1)
        it.push((actor as? Layout)?.minWidth ?: 0f)
        return 1
    }
    
    private fun luaGetMinHeight(it: Lua): Int {
        val actor = it.checkUserdata<Actor>(1)
        it.push((actor as? Layout)?.minHeight ?: 0f)
        return 1
    }
    
    private fun luaGetPreferredWidth(it: Lua): Int {
        val actor = it.checkUserdata<Actor>(1)
        it.push((actor as? Layout)?.prefWidth ?: 0f)
        return 1
    }
    
    private fun luaGetPreferredHeight(it: Lua): Int {
        val actor = it.checkUserdata<Actor>(1)
        it.push((actor as? Layout)?.prefHeight ?: 0f)
        return 1
    }
    
    private fun luaGetMaxWidth(it: Lua): Int {
        val actor = it.checkUserdata<Actor>(1)
        it.push((actor as? Layout)?.maxWidth ?: 0f)
        return 1
    }
    
    private fun luaGetMaxHeight(it: Lua): Int {
        val actor = it.checkUserdata<Actor>(1)
        it.push((actor as? Layout)?.maxHeight ?: 0f)
        return 1
    }
}
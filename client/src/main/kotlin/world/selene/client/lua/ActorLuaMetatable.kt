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
import world.selene.common.lua.checkString
import world.selene.common.lua.checkUserdata

object ActorLuaMetatable {
    val luaMeta = LuaMappedMetatable(Actor::class) {
        getter(Actor::getName)
        getter(Actor::getParent)
        getter(Actor::getWidth)
        getter(Actor::getHeight)
        setter(Actor::setWidth)
        setter(Actor::setHeight)
        callable(::luaInvalidate)
        callable(::luaSetStyle)
        callable(::luaFocus)
        getter("MinWidth") {
            val actor = it.checkSelf()
            it.push((actor as? Layout)?.minWidth ?: 0f)
            1
        }
        getter("MinHeight") {
            val actor = it.checkSelf()
            it.push((actor as? Layout)?.minHeight ?: 0f)
            1
        }
        getter("PreferredWidth") {
            val actor = it.checkSelf()
            it.push((actor as? Layout)?.prefWidth ?: 0f)
            1
        }
        getter("PreferredHeight") {
            val actor = it.checkSelf()
            it.push((actor as? Layout)?.prefHeight ?: 0f)
            1
        }
        getter("MaxWidth") {
            val actor = it.checkSelf()
            it.push((actor as? Layout)?.maxWidth ?: 0f)
            1
        }
        getter("MaxHeight") {
            val actor = it.checkSelf()
            it.push((actor as? Layout)?.maxHeight ?: 0f)
            1
        }
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
}
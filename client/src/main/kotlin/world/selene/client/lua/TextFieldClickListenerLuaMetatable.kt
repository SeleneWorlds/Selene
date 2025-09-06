package world.selene.client.lua

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaMappedMetatable
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkUserdata

object TextFieldClickListenerLuaMetatable {
    val luaMeta = LuaMappedMetatable(TextField.TextFieldClickListener::class) {
        callable(::luaKeyDown)
        callable(::luaKeyUp)
        callable(::luaKeyTyped)
    }

    /**
     * Handles key down events for text field click listeners.
     *
     * ```signatures
     * KeyDown(event: InputEvent, keyCode: number) -> boolean
     * ```
     */
    private fun luaKeyDown(lua: Lua): Int {
        val listener = lua.checkUserdata<ClickListener>(1)
        val event = lua.checkUserdata<InputEvent>(2)
        val keyCode = lua.checkInt(3)
        lua.push(listener.keyDown(event, keyCode))
        return 1
    }

    /**
     * Handles key up events for text field click listeners.
     *
     * ```signatures
     * KeyUp(event: InputEvent, keyCode: number) -> boolean
     * ```
     */
    private fun luaKeyUp(lua: Lua): Int {
        val listener = lua.checkUserdata<ClickListener>(1)
        val event = lua.checkUserdata<InputEvent>(2)
        val keyCode = lua.checkInt(3)
        lua.push(listener.keyUp(event, keyCode))
        return 1
    }

    /**
     * Handles key typed events for text field click listeners.
     *
     * ```signatures
     * KeyTyped(event: InputEvent, char: number) -> boolean
     * ```
     */
    private fun luaKeyTyped(lua: Lua): Int {
        val listener = lua.checkUserdata<ClickListener>(1)
        val event = lua.checkUserdata<InputEvent>(2)
        val char = lua.checkInt(3).toChar()
        lua.push(listener.keyTyped(event, char))
        return 1
    }
}
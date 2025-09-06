package world.selene.client.lua

import com.badlogic.gdx.Gdx
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.input.InputManager
import world.selene.client.input.InputType
import world.selene.common.lua.*

/**
 * Handle input for keyboard and mouse events.
 */
@Suppress("SameReturnValue")
class LuaInputModule(private val inputManager: InputManager) : LuaModule {
    override val name = "selene.input"

    private val inputTypeKeyboard: InputType = InputType.KEYBOARD
    private val inputTypeMouse: InputType = InputType.MOUSE

    override fun register(table: LuaValue) {
        table.register("BindContinuousAction", this::luaBindContinuousAction)
        table.register("BindAction", this::luaBindAction)
        table.register("BindPressAction", this::luaBindPressAction)
        table.register("BindReleaseAction", this::luaBindReleaseAction)
        table.register("IsKeyPressed", this::luaIsKeyPressed)
        table.register("IsMousePressed", this::luaIsMousePressed)
        table.register("GetMousePosition", this::luaGetMousePosition)
        table.set("KEYBOARD", inputTypeKeyboard)
        table.set("MOUSE", inputTypeMouse)
    }

    /**
     * Binds a function to be called continuously while an input is held down.
     * The function is called every frame while the input is active.
     *
     * ```signatures
     * BindContinuousAction(type: InputType, input: string, callback: function)
     * ```
     */
    private fun luaBindContinuousAction(lua: Lua): Int {
        val type = lua.checkEnum<InputType>(1)
        val input = lua.checkString(2)
        lua.pushValue(3)
        val function = lua.get()
        val registrationSite = lua.getCallerInfo()
        val trace = object : LuaTrace {
            override fun luaTrace(): String {
                return "[input \"$input\"] registered in $registrationSite"
            }
        }
        inputManager.bindContinuousAction(type, input) {
            val lua = function.state()
            lua.push(function)
            lua.xpCall(0, 0, trace)
        }
        return 0
    }

    /**
     * Binds a function to be called when an input event occurs.
     * For type `KEYBOARD`: called on key press.
     * For type `MOUSE`: called on click with coordinates.
     *
     * ```signatures
     * BindAction(KEYBOARD: InputType, input: string, callback: function)
     * BindAction(MOUSE: InputType, input: string, callback: function(screenX: number, screenY: number))
     * ```
     */
    private fun luaBindAction(lua: Lua): Int {
        val type = lua.checkEnum<InputType>(1)
        val input = lua.checkString(2)
        lua.pushValue(3)
        val function = lua.get()
        val registrationSite = lua.getCallerInfo()
        val trace = object : LuaTrace {
            override fun luaTrace(): String {
                return "[input \"$input\"] registered in $registrationSite"
            }
        }
        when (type) {
            InputType.KEYBOARD -> {
                val keyCode = inputManager.lookupKeyboardKey(input)
                if (keyCode == -1) {
                    return lua.error(IllegalArgumentException("Unknown keyboard key: $input"))
                }
                inputManager.bindKeyboardAction(keyCode) {
                    val lua = function.state()
                    lua.push(function)
                    lua.xpCall(0, 0, trace)
                }
            }

            InputType.MOUSE -> {
                inputManager.bindMouseAction(input) { screenX, screenY ->
                    val lua = function.state()
                    lua.push(function)
                    lua.push(screenX)
                    lua.push(screenY)
                    lua.xpCall(2, 0, trace)
                }
            }
        }
        return 0
    }

    /**
     * Binds a function to be called when an input is first pressed down.
     * Triggers only once per press, not continuously.
     *
     * ```signatures
     * BindPressAction(KEYBOARD: InputType, input: string, callback: function)
     * BindPressAction(MOUSE: InputType, input: string, callback: function(screenX: number, screenY: number))
     * ```
     */
    private fun luaBindPressAction(lua: Lua): Int {
        val type = lua.checkEnum<InputType>(1)
        val input = lua.checkString(2)
        lua.pushValue(3)
        val function = lua.get()
        val registrationSite = lua.getCallerInfo()
        val trace = object : LuaTrace {
            override fun luaTrace(): String {
                return "[input \"$input\"] registered in $registrationSite"
            }
        }
        when (type) {
            InputType.KEYBOARD -> {
                val keyCode = inputManager.lookupKeyboardKey(input)
                if (keyCode == -1) {
                    return lua.error(IllegalArgumentException("Unknown keyboard key: $input"))
                }
                inputManager.bindKeyboardPressAction(keyCode) {
                    val lua = function.state()
                    lua.push(function)
                    lua.xpCall(0, 0, trace)
                }
            }

            InputType.MOUSE -> {
                inputManager.bindMousePressAction(input) { screenX, screenY ->
                    val lua = function.state()
                    lua.push(function)
                    lua.push(screenX)
                    lua.push(screenY)
                    lua.xpCall(2, 0, trace)
                }
            }
        }
        return 0
    }

    /**
     * Binds a function to be called when an input is released.
     * Triggers when the key/button is let go.
     *
     * ```signatures
     * BindReleaseAction(KEYBOARD: InputType, input: string, callback: function)
     * BindReleaseAction(MOUSE: InputType, input: string, callback: function(screenX: number, screenY: number))
     * ```
     */
    private fun luaBindReleaseAction(lua: Lua): Int {
        val type = lua.checkEnum<InputType>(1)
        val input = lua.checkString(2)
        lua.pushValue(3)
        val function = lua.get()
        val registrationSite = lua.getCallerInfo()
        val trace = object : LuaTrace {
            override fun luaTrace(): String {
                return "[input \"$input\"] registered in $registrationSite"
            }
        }
        when (type) {
            InputType.KEYBOARD -> {
                val keyCode = inputManager.lookupKeyboardKey(input)
                if (keyCode == -1) {
                    return lua.error(IllegalArgumentException("Unknown keyboard key: $input"))
                }
                inputManager.bindKeyboardReleaseAction(keyCode) {
                    val lua = function.state()
                    lua.push(function)
                    lua.xpCall(0, 0, trace)
                }
            }

            InputType.MOUSE -> {
                inputManager.bindMouseReleaseAction(input) { screenX, screenY ->
                    val lua = function.state()
                    lua.push(function)
                    lua.push(screenX)
                    lua.push(screenY)
                    lua.xpCall(2, 0, trace)
                }
            }
        }
        return 0
    }

    /**
     * Checks if a keyboard key is currently pressed.
     *
     * ```signatures
     * IsKeyPressed(key: string) -> boolean
     * ```
     */
    private fun luaIsKeyPressed(lua: Lua): Int {
        val key = lua.checkString(1)
        lua.push(inputManager.isKeyPressed(key))
        return 1
    }

    /**
     * Checks if a mouse button is currently pressed.
     *
     * ```signatures
     * IsMousePressed(button: string) -> boolean
     * ```
     */
    private fun luaIsMousePressed(lua: Lua): Int {
        val button = lua.checkString(1)
        lua.push(inputManager.isMousePressed(button))
        return 1
    }

    /**
     * Returns the current mouse position in screen coordinates.
     *
     * ```signatures
     * GetMousePosition() -> number, number
     * ```
     */
    private fun luaGetMousePosition(lua: Lua): Int {
        lua.push(Gdx.input.x)
        lua.push(Gdx.input.y)
        return 2
    }
}

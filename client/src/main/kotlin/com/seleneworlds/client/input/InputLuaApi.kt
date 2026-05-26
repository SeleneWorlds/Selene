package com.seleneworlds.client.input

import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkEnum
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.common.script.ScriptTrace
import org.slf4j.LoggerFactory
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.value.LuaValue

/**
 * Handle input for keyboard and mouse events.
 */
class InputLuaApi(private val api: InputApi) : LuaModule {
    override val name = "selene.input"

    override fun register(table: LuaValue) {
        table.register("bindContinuousAction", this::bindContinuousAction)
        table.register("bindAction", this::bindAction)
        table.register("bindPressAction", this::bindPressAction)
        table.register("bindReleaseAction", this::bindReleaseAction)
        table.register("isKeyPressed", this::isKeyPressed)
        table.register("isMousePressed", this::isMousePressed)
        table.register("getMousePosition", this::getMousePosition)
        table.set("KEYBOARD", api.inputTypeKeyboard)
        table.set("MOUSE", api.inputTypeMouse)
    }

    private fun bindContinuousAction(lua: Lua): Int {
        val type = lua.checkEnum<InputType>(1)
        val input = lua.checkString(2)
        val function = captureFunction(lua)
        val trace = createTrace(lua, input)
        api.bindContinuousAction(type, input) {
            runInputCallback(function, trace, "continuousAction") { callbackLua ->
                callbackLua.xpCall(0, 0, trace)
            }
        }
        return 0
    }

    private fun bindAction(lua: Lua): Int {
        bindAction(lua) { type, input, keyboardCallback, mouseCallback ->
            api.bindAction(type, input, keyboardCallback, mouseCallback)
        }
        return 0
    }

    private fun bindPressAction(lua: Lua): Int {
        bindAction(lua) { type, input, keyboardCallback, mouseCallback ->
            api.bindPressAction(type, input, keyboardCallback, mouseCallback)
        }
        return 0
    }

    private fun bindReleaseAction(lua: Lua): Int {
        bindAction(lua) { type, input, keyboardCallback, mouseCallback ->
            api.bindReleaseAction(type, input, keyboardCallback, mouseCallback)
        }
        return 0
    }

    private fun isKeyPressed(lua: Lua): Int {
        lua.push(api.isKeyPressed(lua.checkString(1)))
        return 1
    }

    private fun isMousePressed(lua: Lua): Int {
        lua.push(api.isMousePressed(lua.checkString(1)))
        return 1
    }

    private fun getMousePosition(lua: Lua): Int {
        val (x, y) = api.getMousePosition()
        lua.push(x)
        lua.push(y)
        return 2
    }

    private fun bindAction(
        lua: Lua,
        binder: (InputType, String, () -> Unit, (Int, Int) -> Unit) -> Unit
    ) {
        val type = lua.checkEnum<InputType>(1)
        val input = lua.checkString(2)
        val function = captureFunction(lua)
        val trace = createTrace(lua, input)
        binder(
            type,
            input,
            {
                runInputCallback(function, trace, "action") { callbackLua ->
                    callbackLua.xpCall(0, 0, trace)
                }
            },
            { screenX, screenY ->
                runInputCallback(function, trace, "action") { callbackLua ->
                    callbackLua.push(screenX)
                    callbackLua.push(screenY)
                    callbackLua.xpCall(2, 0, trace)
                }
            }
        )
    }

    private fun runInputCallback(
        function: LuaValue,
        trace: ScriptTrace,
        callbackKind: String,
        block: (Lua) -> Unit
    ) {
        val callbackLua = function.state()
        callbackLua.push(function)
        try {
            block(callbackLua)
        } catch (e: LuaException) {
            logger.error("Lua error in input {}", callbackKind, e)
        }
    }

    private fun createTrace(lua: Lua, input: String): ScriptTrace {
        val registrationSite = lua.getCallerInfo()
        return object : ScriptTrace {
            override fun scriptTrace(): String {
                return "[input \"$input\"] registered in $registrationSite"
            }
        }
    }

    private fun captureFunction(lua: Lua): LuaValue {
        lua.pushValue(3)
        return lua.get()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(InputLuaApi::class.java)
    }
}

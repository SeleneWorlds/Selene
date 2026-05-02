package com.seleneworlds.client.input

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.script.ScriptTrace
import com.seleneworlds.common.lua.util.checkEnum
import com.seleneworlds.common.lua.util.checkString
import com.seleneworlds.common.lua.util.getCallerInfo
import com.seleneworlds.common.lua.util.register
import com.seleneworlds.common.lua.util.xpCall

/**
 * Handle input for keyboard and mouse events.
 */
class InputLuaApi(private val api: InputApi) : LuaModule {
    override val name = "selene.input"

    override fun register(table: LuaValue) {
        table.register("BindContinuousAction", this::luaBindContinuousAction)
        table.register("BindAction", this::luaBindAction)
        table.register("BindPressAction", this::luaBindPressAction)
        table.register("BindReleaseAction", this::luaBindReleaseAction)
        table.register("IsKeyPressed", this::luaIsKeyPressed)
        table.register("IsMousePressed", this::luaIsMousePressed)
        table.register("GetMousePosition", this::luaGetMousePosition)
        table.set("KEYBOARD", api.inputTypeKeyboard)
        table.set("MOUSE", api.inputTypeMouse)
    }

    private fun luaBindContinuousAction(lua: Lua): Int {
        val type = lua.checkEnum<InputType>(1)
        val input = lua.checkString(2)
        val function = captureFunction(lua)
        val trace = createTrace(lua, input)
        api.bindContinuousAction(type, input) {
            val callbackLua = function.state()
            callbackLua.push(function)
            callbackLua.xpCall(0, 0, trace)
        }
        return 0
    }

    private fun luaBindAction(lua: Lua): Int {
        bindAction(lua) { type, input, keyboardCallback, mouseCallback ->
            api.bindAction(type, input, keyboardCallback, mouseCallback)
        }
        return 0
    }

    private fun luaBindPressAction(lua: Lua): Int {
        bindAction(lua) { type, input, keyboardCallback, mouseCallback ->
            api.bindPressAction(type, input, keyboardCallback, mouseCallback)
        }
        return 0
    }

    private fun luaBindReleaseAction(lua: Lua): Int {
        bindAction(lua) { type, input, keyboardCallback, mouseCallback ->
            api.bindReleaseAction(type, input, keyboardCallback, mouseCallback)
        }
        return 0
    }

    private fun luaIsKeyPressed(lua: Lua): Int {
        lua.push(api.isKeyPressed(lua.checkString(1)))
        return 1
    }

    private fun luaIsMousePressed(lua: Lua): Int {
        lua.push(api.isMousePressed(lua.checkString(1)))
        return 1
    }

    private fun luaGetMousePosition(lua: Lua): Int {
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
                val callbackLua = function.state()
                callbackLua.push(function)
                callbackLua.xpCall(0, 0, trace)
            },
            { screenX, screenY ->
                val callbackLua = function.state()
                callbackLua.push(function)
                callbackLua.push(screenX)
                callbackLua.push(screenY)
                callbackLua.xpCall(2, 0, trace)
            }
        )
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
}

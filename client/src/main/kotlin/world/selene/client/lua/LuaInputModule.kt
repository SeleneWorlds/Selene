package world.selene.client.lua

import com.badlogic.gdx.Gdx
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.input.InputManager
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkEnum
import world.selene.common.lua.checkString
import world.selene.common.lua.register

class LuaInputModule(private val inputManager: InputManager) : LuaModule {
    override val name = "selene.input"

    override fun register(table: LuaValue) {
        table.register("BindContinuousAction", this::luaBindContinuousAction)
        table.register("GetMousePosition", this::luaGetMousePosition)
    }

    private fun luaBindContinuousAction(lua: Lua): Int {
        val type = lua.checkEnum<InputManager.InputType>(1)
        val input = lua.checkString(2)
        lua.pushValue(3)
        val function = lua.get()
        inputManager.bindContinuousAction(type, input) {
            val lua = function.state()
            lua.push(function)
            lua.pCall(0, 0)
        }
        return 0
    }

    private fun luaGetMousePosition(lua: Lua): Int {
        lua.push(Gdx.input.x)
        lua.push(Gdx.input.y)
        return 2
    }
}

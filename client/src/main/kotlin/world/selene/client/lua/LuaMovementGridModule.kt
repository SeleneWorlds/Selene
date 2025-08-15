package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.controls.GridMovement
import world.selene.common.grid.Grid
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkJavaObject
import world.selene.common.lua.register

class LuaMovementGridModule(private val gridMovement: GridMovement) : LuaModule {
    override val name = "selene.movement.grid"

    override fun register(table: LuaValue) {
        table.register("SetMotion", this::luaSetMotion)
    }

    private fun luaSetMotion(lua: Lua): Int {
        val direction = lua.checkJavaObject(1, Grid.Direction::class)
        gridMovement.moveDirection = direction
        return 0
    }
}

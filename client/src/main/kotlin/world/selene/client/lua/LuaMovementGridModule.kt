package world.selene.client.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.client.controls.GridMovement
import world.selene.common.lua.LuaModule
import world.selene.common.lua.checkInt
import world.selene.common.lua.checkJavaObject
import world.selene.common.lua.register
import world.selene.common.util.Coordinate

class LuaMovementGridModule(private val gridMovement: GridMovement) : LuaModule {
    override val name = "selene.movement.grid"

    override fun register(table: LuaValue) {
        table.register("SetMotion", this::luaSetMotion)
    }

    private fun luaSetMotion(lua: Lua): Int {
        val direction = if (lua.isUserdata(1)) {
            lua.checkJavaObject(1, Coordinate::class)
        } else {
            Coordinate(lua.checkInt(1), lua.checkInt(2), lua.checkInt(3))
        }
        gridMovement.moveDirection = direction
        return 0
    }
}

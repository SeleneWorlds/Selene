package com.seleneworlds.client.controls

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import com.seleneworlds.common.grid.Direction
import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.checkUserdata
import com.seleneworlds.common.lua.util.register

/**
 * Grid-based movement controls.
 */
class MovementGridLuaApi(private val api: MovementGridApi) : LuaModule {
    override val name = "selene.movement.grid"

    override fun register(table: LuaValue) {
        table.register("SetMotion", this::luaSetMotion)
        table.register("SetFacing", this::luaSetFacing)
    }

    private fun luaSetMotion(lua: Lua): Int {
        api.setMotion(lua.checkUserdata(1, Direction::class))
        return 0
    }

    private fun luaSetFacing(lua: Lua): Int {
        api.setFacing(lua.checkUserdata(1, Direction::class))
        return 0
    }
}

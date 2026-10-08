package com.seleneworlds.server.permissions

import com.seleneworlds.common.lua.LuaModule
import com.seleneworlds.common.lua.util.*
import com.seleneworlds.common.script.ConstantTrace
import com.seleneworlds.server.players.PlayerApi
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue

/** Register a global permission handler and check named permissions for players. */
class PermissionsLuaApi(private val api: PermissionsApi) : LuaModule {
    override val name = "selene.permissions"

    override fun register(table: LuaValue) {
        table.register("setHandler", ::setHandler)
        table.register("has", ::has)
    }

    private fun setHandler(lua: Lua): Int {
        val function = lua.checkFunction(1)
        val trace = lua.getCallerInfo()
        api.setHandler { player, permission, context ->
            val state = function.state()
            val previousTop = state.top
            try {
                function.push(state)
                state.push(player, Lua.Conversion.NONE)
                state.push(permission)
                state.push(context)
                state.xpCall(3, 1, ConstantTrace("[permission \"$permission\"] registered in <$trace>"))
                state.type(-1) == Lua.LuaType.BOOLEAN && state.toBoolean(-1)
            } finally {
                state.top = previousTop
            }
        }
        return 0
    }

    private fun has(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        val permission = lua.checkString(2)
        val context = if (lua.isNoneOrNil(3)) emptyMap() else lua.checkSerializedMap(3)
        lua.push(api.hasPermission(player, permission, context))
        return 1
    }
}

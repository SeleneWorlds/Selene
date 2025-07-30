package world.selene.client.lua

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule

class LuaGameModule(private val signals: ClientLuaSignals) : LuaModule {
    override val name = "selene.game"

    override fun register(table: LuaValue) {
        table.set("PreTick", signals.gamePreTick)
    }
}

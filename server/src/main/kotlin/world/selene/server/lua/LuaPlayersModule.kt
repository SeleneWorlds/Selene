package world.selene.server.lua

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule

class LuaPlayersModule(private val signals: ServerLuaSignals) : LuaModule {
    override val name = "selene.players"

    override fun register(table: LuaValue) {
        table.set("PlayerQueued", signals.playerQueued)
        table.set("PlayerDequeued", signals.playerDequeued)
        table.set("PlayerJoined", signals.playerJoined)
        table.set("PlayerLeft", signals.playerLeft)
    }

}

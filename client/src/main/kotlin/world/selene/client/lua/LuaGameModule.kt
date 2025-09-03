package world.selene.client.lua

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.Signal

class LuaGameModule(private val signals: ClientLuaSignals) : LuaModule {
    override val name = "selene.game"

    /**
     * Fired every game tick before any game logic is run.
     */
    private val gamePreTick: Signal = signals.gamePreTick

    override fun register(table: LuaValue) {
        table.set("PreTick", gamePreTick)
    }
}

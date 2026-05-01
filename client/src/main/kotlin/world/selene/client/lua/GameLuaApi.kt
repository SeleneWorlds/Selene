package world.selene.client.lua

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule

/**
 * Game lifecycle signals.
 */
class GameLuaApi(private val api: GameApi) : LuaModule {
    override val name = "selene.game"

    override fun register(table: LuaValue) {
        table.set("PreTick", api.gamePreTick)
    }
}

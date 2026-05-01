package world.selene.client.sounds

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register

/**
 * Play or stop local sounds.
 */
class SoundsLuaApi(
    private val api: SoundsApi
) : LuaModule {
    override val name = "selene.sounds"

    override fun register(table: LuaValue) {
        table.register("PlaySound", api::luaPlaySound)
        table.register("StopSound", api::luaStopSound)
        table.register("StopAllSounds", api::luaStopAllSounds)
    }
}

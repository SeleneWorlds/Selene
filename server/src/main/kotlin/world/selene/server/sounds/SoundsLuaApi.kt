package world.selene.server.sounds

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register

/**
 * Play local, positional or global sounds.
 */
class SoundsLuaApi(
    private val api: SoundsApi
) : LuaModule {
    override val name = "selene.sounds"

    override fun register(table: LuaValue) {
        table.register("PlaySound", api::luaPlaySound)
        table.register("PlaySoundAt", api::luaPlaySoundAt)
        table.register("PlayGlobalSound", api::luaPlayGlobalSound)
    }
}

package world.selene.server.network

import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.register

/**
 * Send and handle custom payloads.
 */
class NetworkLuaApi(private val api: NetworkApi) : LuaModule {
    override val name = "selene.network"

    override fun register(table: LuaValue) {
        table.register("HandlePayload", api::luaHandlePayload)
        table.register("SendToPlayer", api::luaSendToPlayer)
        table.register("SendToPlayers", api::luaSendToPlayers)
        table.register("SendToEntity", api::luaSendToEntity)
        table.register("SendToEntities", api::luaSendToEntities)
    }
}

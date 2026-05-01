package world.selene.server.network

import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.util.checkFunction
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata
import world.selene.common.lua.util.getCallerInfo
import world.selene.common.lua.util.register
import world.selene.common.lua.util.toAnyMap
import world.selene.server.entities.EntityApi
import world.selene.server.players.PlayerApi

/**
 * Send and handle custom payloads.
 */
class NetworkLuaApi(private val api: NetworkApi) : LuaModule {
    override val name = "selene.network"

    override fun register(table: LuaValue) {
        table.register("HandlePayload", ::luaHandlePayload)
        table.register("SendToPlayer", ::luaSendToPlayer)
        table.register("SendToPlayers", ::luaSendToPlayers)
        table.register("SendToEntity", ::luaSendToEntity)
        table.register("SendToEntities", ::luaSendToEntities)
    }

    private fun luaHandlePayload(lua: Lua): Int {
        api.handlePayload(lua.checkString(1), lua.checkFunction(2), lua.getCallerInfo())
        return 0
    }

    private fun luaSendToPlayer(lua: Lua): Int {
        api.sendToPlayer(lua.checkUserdata<PlayerApi>(1), lua.checkString(2), lua.toAnyMap(3))
        return 0
    }

    private fun luaSendToPlayers(lua: Lua): Int {
        val players = lua.toList(1) ?: return lua.error(IllegalArgumentException("Expected list of players"))
        api.sendToPlayers(players, lua.checkString(2), lua.toAnyMap(3))
        return 0
    }

    private fun luaSendToEntity(lua: Lua): Int {
        api.sendToEntity(lua.checkUserdata<EntityApi>(1), lua.checkString(2), lua.toAnyMap(3))
        return 0
    }

    private fun luaSendToEntities(lua: Lua): Int {
        val entities = lua.toList(1) ?: return lua.error(IllegalArgumentException("Expected list of entities"))
        api.sendToEntities(entities, lua.checkString(2), lua.toAnyMap(3))
        return 0
    }
}

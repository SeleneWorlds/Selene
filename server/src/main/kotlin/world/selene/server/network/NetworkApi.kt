package world.selene.server.network

import com.fasterxml.jackson.databind.ObjectMapper
import party.iroiro.luajava.Lua
import world.selene.common.lua.util.checkFunction
import world.selene.common.lua.util.checkString
import world.selene.common.lua.util.checkUserdata
import world.selene.common.lua.util.getCallerInfo
import world.selene.common.lua.util.toAnyMap
import world.selene.common.network.LuaPayloadRegistry
import world.selene.common.network.packet.CustomPayloadPacket
import world.selene.server.entities.EntityApi
import world.selene.server.players.Player
import world.selene.server.players.PlayerApi

/**
 * Send and handle custom payloads.
 */
@Suppress("SameReturnValue")
class NetworkApi(
    private val payloadRegistry: LuaPayloadRegistry,
    private val objectMapper: ObjectMapper
) {
    fun luaHandlePayload(lua: Lua): Int {
        val payloadId = lua.checkString(1)
        val callback = lua.checkFunction(2)
        val registrationSite = lua.getCallerInfo()
        payloadRegistry.registerHandler(payloadId, callback, registrationSite)
        return 0
    }

    fun luaSendToPlayer(lua: Lua): Int {
        val player = lua.checkUserdata<PlayerApi>(1)
        val payloadId = lua.checkString(2)
        val payload = lua.toAnyMap(3)
        player.delegate.client.send(CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload)))
        return 0
    }

    fun luaSendToPlayers(lua: Lua): Int {
        val players = lua.toList(1) ?: return lua.error(IllegalArgumentException("Expected list of players"))
        val payloadId = lua.checkString(2)
        val payload = lua.toAnyMap(3)
        val packet = CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload))
        players.forEach { player ->
            (player as? Player)?.client?.send(packet)
        }
        return 0
    }

    fun luaSendToEntity(lua: Lua): Int {
        val entity = lua.checkUserdata<EntityApi>(1)
        val payloadId = lua.checkString(2)
        val payload = lua.toAnyMap(3)
        val packet = CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload))
        entity.entity.getControllingPlayers().forEach { player ->
            player.client.send(packet)
        }
        return 0
    }

    fun luaSendToEntities(lua: Lua): Int {
        val entities = lua.toList(1) ?: return lua.error(IllegalArgumentException("Expected list of entities"))
        val payloadId = lua.checkString(2)
        val payload = lua.toAnyMap(3)
        val packet = CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload))
        entities.forEach { entity ->
            (entity as? EntityApi)?.entity?.getControllingPlayers()?.forEach { player ->
                player.client.send(packet)
            }
        }
        return 0
    }
}

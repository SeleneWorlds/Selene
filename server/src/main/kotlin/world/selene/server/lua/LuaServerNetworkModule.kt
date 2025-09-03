package world.selene.server.lua

import com.fasterxml.jackson.databind.ObjectMapper
import party.iroiro.luajava.Lua
import party.iroiro.luajava.value.LuaValue
import world.selene.common.lua.LuaModule
import world.selene.common.lua.LuaPayloadRegistry
import world.selene.common.lua.checkFunction
import world.selene.common.lua.checkUserdata
import world.selene.common.lua.checkString
import world.selene.common.lua.getCallerInfo
import world.selene.common.lua.register
import world.selene.common.lua.toAnyMap
import world.selene.common.network.packet.CustomPayloadPacket
import world.selene.server.entities.Entity
import world.selene.server.player.Player

/**
 * Provides networking functions for sending data to players and handling custom payloads.
 */
class LuaServerNetworkModule(private val payloadRegistry: LuaPayloadRegistry, private val objectMapper: ObjectMapper) :
    LuaModule {
    override val name = "selene.network"

    override fun register(table: LuaValue) {
        table.register("HandlePayload", this::luaHandlePayload)
        table.register("SendToPlayer", this::luaSendToPlayer)
        table.register("SendToPlayers", this::luaSendToPlayers)
        table.register("SendToEntity", this::luaSendToEntity)
        table.register("SendToEntities", this::luaSendToEntities)
    }

    /**
     * Registers a handler for incoming custom payloads from clients.
     *
     * ```lua
     * HandlePayload(string payloadId, function(Player player, table payload) callback)
     * ```
     */
    private fun luaHandlePayload(lua: Lua): Int {
        val payloadId = lua.checkString(1)
        val callback = lua.checkFunction(2)
        val registrationSite = lua.getCallerInfo()
        payloadRegistry.registerHandler(payloadId, callback, registrationSite)
        return 0
    }

    /**
     * Sends a custom payload to a specific player.
     *
     * ```lua
     * SendToPlayer(Player player, string payloadId, table payload)
     * ```
     */
    private fun luaSendToPlayer(lua: Lua): Int {
        val player = lua.checkUserdata<Player>(1)
        val payloadId = lua.checkString(2)
        val payload = lua.toAnyMap(3)
        player.client.send(CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload)))
        return 0
    }

    /**
     * Sends a custom payload to multiple players.
     *
     * ```lua
     * SendToPlayers(table(Player) players, string payloadId, table payload)
     * ```
     */
    private fun luaSendToPlayers(lua: Lua): Int {
        val players = lua.toList(1) ?: return lua.error(IllegalArgumentException("Expected list of players"))
        val payloadId = lua.checkString(2)
        val payload = lua.toAnyMap(3)
        val packet = CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload))
        players.forEach { player ->
            (player as? Player)?.client?.send(packet)
        }
        return 0
    }

    /**
     * Sends a custom payload to all players controlling an entity.
     *
     * ```lua
     * SendToEntity(Entity entity, string payloadId, table payload)
     * ```
     */
    private fun luaSendToEntity(lua: Lua): Int {
        val entity = lua.checkUserdata<Entity>(1)
        val payloadId = lua.checkString(2)
        val payload = lua.toAnyMap(3)
        val packet = CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload))
        entity.getControllingPlayers().forEach { player ->
            player.client.send(packet)
        }
        return 0
    }

    /**
     * Sends a custom payload to all players controlling multiple entities.
     *
     * ```lua
     * SendToEntities(table(Entity) entities, string payloadId, table payload)
     * ```
     */
    private fun luaSendToEntities(lua: Lua): Int {
        val entities = lua.toList(1) ?: return lua.error(IllegalArgumentException("Expected list of players"))
        val payloadId = lua.checkString(2)
        val payload = lua.toAnyMap(3)
        val packet = CustomPayloadPacket(payloadId, objectMapper.writeValueAsString(payload))
        entities.forEach { entity ->
            (entity as? Entity)?.getControllingPlayers()?.forEach { player ->
                player.client.send(packet)
            }
        }
        return 0
    }

}

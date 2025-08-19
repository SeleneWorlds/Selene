package world.selene.server.network

import org.slf4j.Logger
import party.iroiro.luajava.Lua
import world.selene.common.data.NameIdRegistry
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaPayloadRegistry
import world.selene.common.network.Packet
import world.selene.common.network.PacketHandler
import world.selene.common.network.packet.AuthenticatePacket
import world.selene.common.network.packet.CustomPayloadPacket
import world.selene.common.network.packet.MoveEntityPacket
import world.selene.common.network.packet.NameIdMappingsPacket
import world.selene.common.network.packet.PreferencesPacket
import world.selene.common.network.packet.RequestMovePacket
import world.selene.server.login.SessionAuthentication
import world.selene.server.lua.ServerLuaSignals
import java.util.Locale

class ServerPacketHandler(
    private val logger: Logger,
    private val signals: ServerLuaSignals,
    private val nameIdRegistry: NameIdRegistry,
    private val luaManager: LuaManager,
    private val payloadRegistry: LuaPayloadRegistry,
    private val sessionAuthentication: SessionAuthentication
) : PacketHandler<NetworkClient> {
    override fun handle(
        context: NetworkClient,
        packet: Packet
    ) {
        if (packet is AuthenticatePacket) {
            val player = (context as NetworkClientImpl).player
            val tokenData = sessionAuthentication.parseToken(packet.token)
            player.userId = tokenData.userId
            for (scope in nameIdRegistry.mappings.rowKeySet()) {
                val mappings = nameIdRegistry.mappings.row(scope)
                mappings.entries.windowed(500, partialWindows = true).forEach { chunk ->
                    context.send(NameIdMappingsPacket(scope, chunk))
                }
            }
            signals.playerJoined.emit { lua ->
                lua.push(player, Lua.Conversion.NONE)
                1
            }
        } else if (packet is PreferencesPacket) {
            val player = (context as NetworkClientImpl).player
            val localeParts = packet.locale.split("_")
            player.locale = when (localeParts.size) {
                1 -> Locale.of(localeParts[0])
                2 -> Locale.of(localeParts[0], localeParts[1])
                3 -> Locale.of(localeParts[0], localeParts[1], localeParts[2])
                else -> Locale.ENGLISH
            }
        } else if (packet is RequestMovePacket) {
            val player = (context as NetworkClientImpl).player
            player.resetLastInputTime()
            val controlledEntity = player.controlledEntity ?: return
            if (!controlledEntity.moveTo(packet.coordinate)) {
                context.send(
                    MoveEntityPacket(
                        controlledEntity.networkId,
                        controlledEntity.coordinate,
                        controlledEntity.coordinate,
                        controlledEntity.facing?.angle ?: 0f,
                        0f
                    )
                )
            }
        } else if (packet is CustomPayloadPacket) {
            context.enqueueWork {
                val handler = payloadRegistry.retrieveHandler(packet.payloadId)
                if (handler != null) {
                    val player = (context as NetworkClientImpl).player
                    handler.push(luaManager.lua)
                    luaManager.lua.push(player, Lua.Conversion.NONE)
                    luaManager.lua.newTable()
                    // Helper to recursively push Map<String, Any> to Lua table
                    fun pushMapToLuaTable(map: Map<String, Any>) {
                        for ((key, value) in map) {
                            when (value) {
                                is Int -> luaManager.lua.push(value)
                                is Float -> luaManager.lua.push(value)
                                is Double -> luaManager.lua.push(value)
                                is String -> luaManager.lua.push(value)
                                is Boolean -> luaManager.lua.push(value)
                                is Map<*, *> -> {
                                    luaManager.lua.newTable()
                                    @Suppress("UNCHECKED_CAST")
                                    pushMapToLuaTable(value as Map<String, Any>)
                                }

                                else -> luaManager.lua.push(value.toString()) // fallback
                            }
                            luaManager.lua.setField(-2, key)
                        }
                    }
                    pushMapToLuaTable(packet.payload)
                    try {
                        luaManager.lua.pCall(2, 0)
                    } catch (e: Exception) {
                        logger.error("Error while handling custom payload", luaManager.sanitizeException(e))
                    }
                }
            }
        }
    }
}
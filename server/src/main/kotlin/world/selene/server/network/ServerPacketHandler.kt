package world.selene.server.network

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import world.selene.common.data.NameIdRegistry
import world.selene.common.grid.Grid
import world.selene.common.lua.LuaManager
import world.selene.common.lua.LuaPayloadRegistry
import world.selene.common.network.Packet
import world.selene.common.network.PacketHandler
import world.selene.common.network.packet.AuthenticatePacket
import world.selene.common.network.packet.CustomPayloadPacket
import world.selene.common.network.packet.FinalizeJoinPacket
import world.selene.common.network.packet.MoveEntityPacket
import world.selene.common.network.packet.NameIdMappingsPacket
import world.selene.common.network.packet.PreferencesPacket
import world.selene.common.network.packet.RequestFacingPacket
import world.selene.common.network.packet.RequestMovePacket
import world.selene.common.network.packet.TurnEntityPacket
import world.selene.server.login.SessionAuthentication
import world.selene.server.lua.ServerLuaSignals
import world.selene.server.player.Player
import java.util.Locale

class ServerPacketHandler(
    private val logger: Logger,
    private val objectMapper: ObjectMapper,
    private val signals: ServerLuaSignals,
    private val nameIdRegistry: NameIdRegistry,
    private val luaManager: LuaManager,
    private val payloadRegistry: LuaPayloadRegistry,
    private val grid: Grid,
    private val sessionAuthentication: SessionAuthentication
) : PacketHandler<NetworkClient> {

    private fun handleAuthentication(context: NetworkClient, packet: Packet) {
        val player = (context as NetworkClientImpl).player
        if (packet is AuthenticatePacket) {
            val tokenData = sessionAuthentication.parseToken(packet.token)
            player.userId = tokenData.userId
            for (scope in nameIdRegistry.mappings.rowKeySet()) {
                val mappings = nameIdRegistry.mappings.row(scope)
                mappings.entries.windowed(500, partialWindows = true).forEach { chunk ->
                    context.send(NameIdMappingsPacket(scope, chunk))
                }
                context.send(NameIdMappingsPacket(scope, emptyList()))
            }
            player.connectionState = Player.ConnectionState.PENDING_JOIN
        }
    }

    private fun handlePreferences(context: NetworkClient, packet: PreferencesPacket) {
        val player = (context as NetworkClientImpl).player
        val localeParts = packet.locale.split("_")
        player.locale = when (localeParts.size) {
            1 -> Locale.of(localeParts[0])
            2 -> Locale.of(localeParts[0], localeParts[1])
            3 -> Locale.of(localeParts[0], localeParts[1], localeParts[2])
            else -> Locale.ENGLISH
        }
    }

    private fun handleJoin(context: NetworkClient, packet: Packet) {
        if (packet is PreferencesPacket) {
            handlePreferences(context, packet)
        } else if (packet is FinalizeJoinPacket) {
            val player = (context as NetworkClientImpl).player
            player.connectionState = Player.ConnectionState.READY
            signals.playerJoined.emit { lua ->
                lua.push(player, Lua.Conversion.NONE)
                1
            }
        }
    }

    private fun handleGame(context: NetworkClient, packet: Packet) {
        val player = (context as NetworkClientImpl).player
        if (packet is RequestMovePacket) {
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
        } else if (packet is RequestFacingPacket) {
            player.resetLastInputTime()
            val controlledEntity = player.controlledEntity ?: return
            val facing = grid.getDirection(packet.angle)
            if (controlledEntity.facing != facing) {
                controlledEntity.turnTo(facing)
            }
        } else if (packet is CustomPayloadPacket) {
            context.enqueueWork {
                val handler = payloadRegistry.retrieveHandler(packet.payloadId)
                if (handler != null) {
                    handler.callback.push(luaManager.lua)
                    luaManager.lua.push(player, Lua.Conversion.NONE)
                    val payload = objectMapper.readValue(packet.payload, Map::class.java)
                    luaManager.lua.push(payload)
                    try {
                        luaManager.lua.pCall(2, 0)
                    } catch (e: LuaException) {
                        logger.error(
                            "Error while handling custom payload ${packet.payloadId} (registered at ${handler.registrationSite})",
                            e
                        )
                    }
                }
            }
        }
    }

    override fun handle(
        context: NetworkClient,
        packet: Packet
    ) {
        val player = (context as NetworkClientImpl).player
        when (player.connectionState) {
            Player.ConnectionState.PENDING_AUTHENTICATION -> handleAuthentication(context, packet)
            Player.ConnectionState.PENDING_JOIN -> handleJoin(context, packet)
            Player.ConnectionState.READY -> handleGame(context, packet)
        }
    }
}

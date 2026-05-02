package com.seleneworlds.server.network

import kotlinx.serialization.json.Json
import org.slf4j.Logger
import com.seleneworlds.common.data.mappings.NameIdRegistry
import com.seleneworlds.common.grid.Grid
import com.seleneworlds.common.network.PayloadHandlerRegistry
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.PacketHandler
import com.seleneworlds.common.network.packet.*
import com.seleneworlds.common.serialization.SerializedMapSerializer
import com.seleneworlds.server.login.SessionAuthentication
import com.seleneworlds.server.players.Player
import com.seleneworlds.server.players.PlayerEvents
import java.util.*

class ServerPacketHandler(
    private val logger: Logger,
    private val json: Json,
    private val nameIdRegistry: NameIdRegistry,
    private val payloadRegistry: PayloadHandlerRegistry<Player>,
    private val grid: Grid,
    private val sessionAuthentication: SessionAuthentication
) : PacketHandler<NetworkClient> {

    private fun handleAuthentication(context: NetworkClient, packet: Packet) {
        val player = (context as NetworkClientImpl).player
        if (packet is AuthenticatePacket) {
            sessionAuthentication.parseToken(packet.token)
                .onRight {
                    player.userId = it.userId
                    for (scope in nameIdRegistry.mappings.rowKeySet()) {
                        val mappings = nameIdRegistry.mappings.row(scope)
                        mappings.entries.windowed(500, partialWindows = true).forEach { chunk ->
                            context.send(NameIdMappingsPacket(scope, chunk))
                        }
                        context.send(NameIdMappingsPacket(scope, emptyList()))
                    }
                    player.connectionState = Player.ConnectionState.PENDING_JOIN
                }
                .onLeft {
                    logger.warn("Invalid authentication token for ${context.address}", it)
                    player.connectionState = Player.ConnectionState.DISCONNECTED
                    context.send(DisconnectPacket("Invalid authentication token"))
                    context.disconnect()
                }
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
            PlayerEvents.PlayerJoined.EVENT.invoker().playerJoined(player.api)
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
                val handler = payloadRegistry.getHandler(packet.payloadId)
                if (handler != null) {
                    val payload = json.decodeFromString(SerializedMapSerializer, packet.payload)
                    handler(player, payload)
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
            Player.ConnectionState.DISCONNECTED -> {}
        }
    }
}

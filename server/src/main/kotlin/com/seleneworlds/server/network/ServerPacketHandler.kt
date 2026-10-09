package com.seleneworlds.server.network

import com.seleneworlds.common.data.mappings.NameIdRegistry
import com.seleneworlds.common.grid.ActiveGrid
import com.seleneworlds.common.grid.Grid
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.PacketHandler
import com.seleneworlds.common.network.PayloadHandlerRegistry
import com.seleneworlds.common.network.packet.*
import com.seleneworlds.common.serialization.SerializedMapSerializer
import com.seleneworlds.server.login.SessionAuthentication
import com.seleneworlds.server.players.Player
import com.seleneworlds.server.players.PlayerEvents
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import java.util.*

class ServerPacketHandler(
    private val logger: Logger,
    private val json: Json,
    private val nameIdRegistry: NameIdRegistry,
    private val payloadRegistry: PayloadHandlerRegistry<Player>,
    private val grid: Grid,
    private val activeGrid: ActiveGrid,
    private val sessionAuthentication: SessionAuthentication
) : PacketHandler<NetworkClient> {

    private fun handleAuthentication(context: NetworkClient, packet: Packet) {
        val player = (context as NetworkPlayerClient).player
        if (packet is AuthenticatePacket) {
            sessionAuthentication.parseGameToken(packet.token)
                .onRight {
                    player.userId = it.userId
                    for (scope in nameIdRegistry.mappings.rowKeySet()) {
                        val mappings = nameIdRegistry.mappings.row(scope)
                        mappings.entries.chunked(500).forEach { chunk ->
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
        val player = (context as NetworkPlayerClient).player
        player.locale = parseClientLocale(packet.locale)
            ?: throw IllegalArgumentException("Invalid locale")
    }

    private fun handleJoin(context: NetworkClient, packet: Packet) {
        if (packet is PreferencesPacket) {
            handlePreferences(context, packet)
        } else if (packet is FinalizeJoinPacket) {
            val player = (context as NetworkPlayerClient).player
            val activeGridId = activeGrid.activeGridId
                ?: throw IllegalStateException("Active grid was not initialized before player join")
            context.send(SetActiveGridPacket(activeGridId.toString()))
            player.connectionState = Player.ConnectionState.READY
            PlayerEvents.PlayerJoined.EVENT.invoker().playerJoined(player.api)
        }
    }

    private fun handleGame(context: NetworkClient, packet: Packet) {
        val player = (context as NetworkPlayerClient).player
        if (packet is RequestMovePacket) {
            player.resetLastInputTime()
            val controlledEntity = player.controlledEntity ?: return
            val isAllowedStep = grid.isAllowedStep(controlledEntity.coordinate, packet.coordinate)
            val movementDuration = if (isAllowedStep) {
                controlledEntity.getMovementDuration(packet.coordinate)
            } else {
                null
            }
            if (movementDuration == null ||
                !player.acquireMoveRequestCooldown(movementDuration) ||
                !controlledEntity.moveTo(packet.coordinate, movementDuration)
            ) {
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
            if (!player.canTurn) {
                context.send(
                    TurnEntityPacket(
                        controlledEntity.networkId,
                        controlledEntity.facing?.angle ?: 0f
                    )
                )
                return
            }
            val facing = grid.getDirection(packet.angle)
            if (controlledEntity.facing != facing) {
                controlledEntity.turnTo(facing)
            }
        } else if (packet is CustomPayloadPacket) {
            context.enqueueWork {
                if (payloadRegistry.hasHandlers(packet.payloadId)) {
                    try {
                        val payload = json.decodeFromString(SerializedMapSerializer, packet.payload)
                        payloadRegistry.dispatch(packet.payloadId, player, payload)
                    } catch (e: Exception) {
                        logger.warn("Failed to handle custom payload {} from {}", packet.payloadId, context.address, e)
                    }
                }
            }
        }
    }

    override fun handle(
        context: NetworkClient,
        packet: Packet
    ) {
        val player = (context as NetworkPlayerClient).player
        when (player.connectionState) {
            Player.ConnectionState.PENDING_AUTHENTICATION -> handleAuthentication(context, packet)
            Player.ConnectionState.PENDING_JOIN -> handleJoin(context, packet)
            Player.ConnectionState.READY -> handleGame(context, packet)
            Player.ConnectionState.DISCONNECTED -> {}
        }
    }

    internal companion object {
        private val CLIENT_LOCALE_PATTERN = Regex(
            "^([A-Za-z]{2,8})(?:[-_]([A-Za-z]{2}|[0-9]{3}))?(?:[-_]([A-Za-z0-9]{1,8}))?$"
        )

        internal fun parseClientLocale(value: String): Locale? {
            val match = CLIENT_LOCALE_PATTERN.matchEntire(value) ?: return null
            val (language, country, variant) = match.destructured
            return when {
                variant.isNotEmpty() -> Locale.of(language, country, variant)
                country.isNotEmpty() -> Locale.of(language, country)
                else -> Locale.of(language)
            }
        }
    }
}

package com.seleneworlds.server.timeline

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.network.packet.PlayTimelinePacket
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.toJsonElement
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.players.PlayerApi
import com.seleneworlds.server.world.World

/** Starts client-defined timelines for one player or observers of a world position. */
class TimelinesApi(private val world: World) {
    fun play(
        player: PlayerApi,
        timeline: String,
        parameters: SerializedMap = emptyMap(),
    ) {
        player.delegate.client.send(
            packet(timeline, parameters)
        )
    }

    fun playAt(
        position: Coordinate,
        timeline: String,
        dimension: Dimension = world.dimensionManager.getOrCreateDimension(0),
        parameters: SerializedMap = emptyMap(),
    ) {
        dimension.syncManager.sendToAllWatching(
            position,
            packet(timeline, parameters + ("position" to mapOf("x" to position.x, "y" to position.y, "z" to position.z)))
        )
    }

    private fun packet(timeline: String, parameters: SerializedMap) = PlayTimelinePacket(
        Identifier.parse(timeline).toString(),
        parameters.toJsonElement()
    )
}

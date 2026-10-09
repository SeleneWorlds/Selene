package com.seleneworlds.server.timeline

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.network.packet.PlayTimelinePacket
import com.seleneworlds.common.network.packet.StopTimelinePacket
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.toJsonElement
import com.seleneworlds.server.dimensions.Dimension
import com.seleneworlds.server.players.PlayerApi
import com.seleneworlds.server.world.World
import java.util.UUID
import com.seleneworlds.common.timeline.TimelinePlaybackOptions

/** Starts client-defined timelines for one player or observers of a world position. */
class TimelinesApi(private val world: World) {
    fun play(
        player: PlayerApi,
        timeline: String,
        parameters: SerializedMap = emptyMap(),
        tags: List<String> = emptyList(),
        options: TimelinePlaybackOptions = TimelinePlaybackOptions(),
    ): String {
        val instanceId = options.instanceId ?: UUID.randomUUID().toString()
        player.delegate.client.send(
            packet(instanceId, timeline, parameters, tags, options.transition)
        )
        return instanceId
    }

    fun playAt(
        position: Coordinate,
        timeline: String,
        dimension: Dimension = world.dimensionManager.getOrCreateDimension(0),
        parameters: SerializedMap = emptyMap(),
        tags: List<String> = emptyList(),
        options: TimelinePlaybackOptions = TimelinePlaybackOptions(),
    ): String {
        val instanceId = options.instanceId ?: UUID.randomUUID().toString()
        dimension.syncManager.sendToAllWatching(
            position,
            packet(
                instanceId,
                timeline,
                parameters + ("position" to mapOf("x" to position.x, "y" to position.y, "z" to position.z)),
                tags,
                options.transition,
            )
        )
        return instanceId
    }

    fun stop(player: PlayerApi, instanceId: String) {
        player.delegate.client.send(StopTimelinePacket(instanceId = instanceId))
    }

    fun stopAll(player: PlayerApi, timeline: String) {
        player.delegate.client.send(StopTimelinePacket(timeline = Identifier.parse(timeline).toString()))
    }

    fun stopTag(player: PlayerApi, tag: String) {
        player.delegate.client.send(StopTimelinePacket(tag = Identifier.parse(tag).toString()))
    }

    fun stopAt(
        position: Coordinate,
        instanceId: String,
        dimension: Dimension = world.dimensionManager.getOrCreateDimension(0),
    ) {
        dimension.syncManager.sendToAllWatching(position, StopTimelinePacket(instanceId = instanceId))
    }

    fun stopAllAt(
        position: Coordinate,
        timeline: String,
        dimension: Dimension = world.dimensionManager.getOrCreateDimension(0),
    ) {
        dimension.syncManager.sendToAllWatching(
            position,
            StopTimelinePacket(timeline = Identifier.parse(timeline).toString())
        )
    }

    fun stopTagAt(
        position: Coordinate,
        tag: String,
        dimension: Dimension = world.dimensionManager.getOrCreateDimension(0),
    ) {
        dimension.syncManager.sendToAllWatching(
            position,
            StopTimelinePacket(tag = Identifier.parse(tag).toString())
        )
    }

    private fun packet(
        instanceId: String,
        timeline: String,
        parameters: SerializedMap,
        tags: List<String>,
        transition: Float,
    ) = PlayTimelinePacket(
        instanceId,
        Identifier.parse(timeline).toString(),
        parameters.toJsonElement(),
        tags.map { Identifier.parse(it).toString() }.distinct(),
        transition,
    )
}

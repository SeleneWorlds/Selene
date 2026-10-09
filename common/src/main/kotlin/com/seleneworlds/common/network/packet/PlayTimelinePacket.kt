package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readString
import com.seleneworlds.common.network.writeString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** Starts a client-defined timeline with values supplied by the server. */
data class PlayTimelinePacket(
    val instanceId: String,
    val timeline: String,
    /** Arbitrary values interpreted by the client-side event implementations. */
    val parameters: JsonObject = JsonObject(emptyMap()),
    val tags: List<String> = emptyList(),
    /** Seconds spent transitioning numeric parameters of an existing instance. */
    val transition: Float = 0f,
) : Packet {
    init {
        require(instanceId.isNotBlank()) { "instanceId must not be blank" }
        require(transition.isFinite() && transition >= 0f) { "transition must be a non-negative finite number" }
    }

    companion object {
        fun decode(buf: ByteBuf): PlayTimelinePacket {
            val instanceId = buf.readString()
            val timeline = buf.readString()
            val parameters = Json.parseToJsonElement(buf.readString()).jsonObject
            val tags = List(buf.readInt()) { buf.readString() }
            return PlayTimelinePacket(instanceId, timeline, parameters, tags, buf.readFloat())
        }

        fun encode(buf: ByteBuf, packet: PlayTimelinePacket) {
            buf.writeString(packet.instanceId)
            buf.writeString(packet.timeline)
            buf.writeString(Json.encodeToString(JsonObject.serializer(), packet.parameters))
            buf.writeInt(packet.tags.size)
            packet.tags.forEach { buf.writeString(it) }
            buf.writeFloat(packet.transition)
        }
    }
}

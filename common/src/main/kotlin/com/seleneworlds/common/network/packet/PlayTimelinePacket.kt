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
    val timeline: String,
    /** Arbitrary values interpreted by the client-side event implementations. */
    val parameters: JsonObject = JsonObject(emptyMap()),
) : Packet {
    companion object {
        fun decode(buf: ByteBuf): PlayTimelinePacket {
            val timeline = buf.readString()
            return PlayTimelinePacket(timeline, Json.parseToJsonElement(buf.readString()).jsonObject)
        }

        fun encode(buf: ByteBuf, packet: PlayTimelinePacket) {
            buf.writeString(packet.timeline)
            buf.writeString(Json.encodeToString(JsonObject.serializer(), packet.parameters))
        }
    }
}

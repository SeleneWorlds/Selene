package com.seleneworlds.common.network.packet

import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readString
import com.seleneworlds.common.network.writeString
import io.netty.buffer.ByteBuf

/** Stops one timeline instance, or every active instance of a timeline definition. */
data class StopTimelinePacket(
    val instanceId: String? = null,
    val timeline: String? = null,
) : Packet {
    init {
        require((instanceId == null) != (timeline == null)) { "Exactly one timeline stop target is required" }
    }

    companion object {
        fun decode(buf: ByteBuf): StopTimelinePacket {
            val instanceId = buf.readString().ifEmpty { null }
            val timeline = buf.readString().ifEmpty { null }
            return StopTimelinePacket(instanceId, timeline)
        }

        fun encode(buf: ByteBuf, packet: StopTimelinePacket) {
            buf.writeString(packet.instanceId.orEmpty())
            buf.writeString(packet.timeline.orEmpty())
        }
    }
}

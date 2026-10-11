package com.seleneworlds.common.network.packet

import com.seleneworlds.common.network.Packet
import io.netty.buffer.ByteBuf

/** Delimits a camera chunk window, including windows containing only empty cells. */
data class MapSyncStatePacket(val complete: Boolean, val hideWorld: Boolean = false) : Packet {
    companion object {
        fun decode(buf: ByteBuf) = MapSyncStatePacket(buf.readBoolean(), buf.readBoolean())
        fun encode(buf: ByteBuf, packet: MapSyncStatePacket) {
            buf.writeBoolean(packet.complete)
            buf.writeBoolean(packet.hideWorld)
        }
    }
}

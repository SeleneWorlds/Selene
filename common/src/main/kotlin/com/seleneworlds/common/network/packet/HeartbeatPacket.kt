package com.seleneworlds.common.network.packet

import com.seleneworlds.common.network.Packet
import io.netty.buffer.ByteBuf

/** Server probe; clients echo it immediately, independently of the game loop. */
class HeartbeatPacket : Packet {
    companion object {
        fun decode(@Suppress("unused") buf: ByteBuf) = HeartbeatPacket()
        fun encode(@Suppress("unused") buf: ByteBuf, @Suppress("unused") packet: HeartbeatPacket) = Unit
    }
}

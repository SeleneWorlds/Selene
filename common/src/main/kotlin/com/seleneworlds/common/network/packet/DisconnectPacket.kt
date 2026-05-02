package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readString
import com.seleneworlds.common.network.writeString

data class DisconnectPacket(val reason: String) : Packet {

    companion object {
        fun decode(buf: ByteBuf): DisconnectPacket {
            val reason = buf.readString()
            return DisconnectPacket(reason)
        }

        fun encode(buf: ByteBuf, packet: DisconnectPacket) {
            buf.writeString(packet.reason)
        }
    }
}
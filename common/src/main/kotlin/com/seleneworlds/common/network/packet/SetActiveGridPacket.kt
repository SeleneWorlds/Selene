package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readString
import com.seleneworlds.common.network.writeString

data class SetActiveGridPacket(val identifier: String) : Packet {

    companion object {
        fun decode(buf: ByteBuf): SetActiveGridPacket {
            return SetActiveGridPacket(buf.readString())
        }

        fun encode(buf: ByteBuf, packet: SetActiveGridPacket) {
            buf.writeString(packet.identifier)
        }
    }
}

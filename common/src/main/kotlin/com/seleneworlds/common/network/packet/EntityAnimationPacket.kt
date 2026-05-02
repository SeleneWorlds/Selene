package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readString
import com.seleneworlds.common.network.writeString

data class EntityAnimationPacket(val networkId: Int, val animation: String) : Packet {
    companion object {
        fun decode(buf: ByteBuf): EntityAnimationPacket {
            val networkId = buf.readInt()
            val animation = buf.readString()
            return EntityAnimationPacket(networkId, animation)
        }

        fun encode(buf: ByteBuf, packet: EntityAnimationPacket) {
            buf.writeInt(packet.networkId)
            buf.writeString(packet.animation)
        }
    }
}
package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet

/**
 * Packet to inform the client which entity it controls.
 */
data class SetControlledEntityPacket(val networkId: Int) : Packet {
    companion object {
        fun decode(buf: ByteBuf): SetControlledEntityPacket {
            val networkId = buf.readInt()
            return SetControlledEntityPacket(networkId)
        }

        fun encode(buf: ByteBuf, packet: SetControlledEntityPacket) {
            buf.writeInt(packet.networkId)
        }
    }
}

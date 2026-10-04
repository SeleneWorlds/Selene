package com.seleneworlds.common.network.packet

import com.seleneworlds.common.network.Packet
import io.netty.buffer.ByteBuf

data class SetEnvironmentLightPacket(
    val red: Float,
    val green: Float,
    val blue: Float,
) : Packet {
    companion object {
        fun decode(buf: ByteBuf) = SetEnvironmentLightPacket(buf.readFloat(), buf.readFloat(), buf.readFloat())

        fun encode(buf: ByteBuf, packet: SetEnvironmentLightPacket) {
            buf.writeFloat(packet.red)
            buf.writeFloat(packet.green)
            buf.writeFloat(packet.blue)
        }
    }
}

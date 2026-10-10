package com.seleneworlds.common.network.packet

import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readString
import com.seleneworlds.common.network.writeString
import io.netty.buffer.ByteBuf

data class SetEnvironmentLightPacket(
    val name: String,
    val red: Float,
    val green: Float,
    val blue: Float,
) : Packet {
    companion object {
        fun decode(buf: ByteBuf) = SetEnvironmentLightPacket(buf.readString(), buf.readFloat(), buf.readFloat(), buf.readFloat())

        fun encode(buf: ByteBuf, packet: SetEnvironmentLightPacket) {
            buf.writeString(packet.name)
            buf.writeFloat(packet.red)
            buf.writeFloat(packet.green)
            buf.writeFloat(packet.blue)
        }
    }
}

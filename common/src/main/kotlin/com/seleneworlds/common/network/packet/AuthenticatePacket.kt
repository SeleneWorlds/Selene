package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readString
import com.seleneworlds.common.network.writeString

data class AuthenticatePacket(val token: String) : Packet {

    companion object {
        fun decode(buf: ByteBuf): AuthenticatePacket {
            val token = buf.readString()
            return AuthenticatePacket(token)
        }

        fun encode(buf: ByteBuf, packet: AuthenticatePacket) {
            buf.writeString(packet.token)
        }
    }
}
package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readString
import world.selene.common.network.writeString

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
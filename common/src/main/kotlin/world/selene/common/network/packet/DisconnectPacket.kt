package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readString
import world.selene.common.network.writeString

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
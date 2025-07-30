package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readString
import world.selene.common.network.writeString

data class CommandResponsePacket(val success: Boolean, val response: String) : Packet {

    companion object {
        fun decode(buf: ByteBuf): CommandResponsePacket {
            val success = buf.readBoolean()
            val response = buf.readString()
            return CommandResponsePacket(success, response)
        }

        fun encode(buf: ByteBuf, packet: CommandResponsePacket) {
            buf.writeBoolean(packet.success)
            buf.writeString(packet.response)
        }
    }
}
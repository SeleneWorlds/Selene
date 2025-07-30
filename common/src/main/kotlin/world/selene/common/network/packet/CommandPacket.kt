package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readString
import world.selene.common.network.writeString

data class CommandPacket(val command: String) : Packet {

    companion object {
        fun decode(buf: ByteBuf): CommandPacket {
            val command = buf.readString()
            return CommandPacket(command)
        }

        fun encode(buf: ByteBuf, packet: CommandPacket) {
            buf.writeString(packet.command)
        }
    }
}
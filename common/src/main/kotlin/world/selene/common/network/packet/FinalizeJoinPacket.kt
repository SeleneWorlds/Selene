package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet

class FinalizeJoinPacket : Packet {

    companion object {
        fun decode(buf: ByteBuf): FinalizeJoinPacket {
            return FinalizeJoinPacket()
        }

        fun encode(buf: ByteBuf, packet: FinalizeJoinPacket) {
        }
    }
}
package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet

data class TurnEntityPacket(val networkId: Int, val facing: Float) : Packet {
    companion object {
        fun decode(buf: ByteBuf): TurnEntityPacket {
            val networkId = buf.readInt()
            val facing = buf.readFloat()
            return TurnEntityPacket(networkId, facing)
        }

        fun encode(buf: ByteBuf, packet: TurnEntityPacket) {
            buf.writeInt(packet.networkId)
            buf.writeFloat(packet.facing)
        }
    }
}
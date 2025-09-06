package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet

data class RemoveEntityPacket(val networkId: Int) : Packet {
    companion object {
        fun encode(buf: ByteBuf, packet: RemoveEntityPacket) {
            buf.writeInt(packet.networkId)
        }

        fun decode(buf: ByteBuf): RemoveEntityPacket {
            val networkId = buf.readInt()
            return RemoveEntityPacket(networkId)
        }
    }
}

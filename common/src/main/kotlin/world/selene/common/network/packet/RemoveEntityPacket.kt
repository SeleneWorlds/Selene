package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readCoordinate
import world.selene.common.network.readString
import world.selene.common.network.writeCoordinate
import world.selene.common.network.writeString
import world.selene.common.util.Coordinate

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

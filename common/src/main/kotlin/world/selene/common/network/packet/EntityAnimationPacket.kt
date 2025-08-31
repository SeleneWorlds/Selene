package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readString
import world.selene.common.network.writeString

data class EntityAnimationPacket(val networkId: Int, val animation: String) : Packet {
    companion object {
        fun decode(buf: ByteBuf): EntityAnimationPacket {
            val networkId = buf.readInt()
            val animation = buf.readString()
            return EntityAnimationPacket(networkId, animation)
        }

        fun encode(buf: ByteBuf, packet: EntityAnimationPacket) {
            buf.writeInt(packet.networkId)
            buf.writeString(packet.animation)
        }
    }
}
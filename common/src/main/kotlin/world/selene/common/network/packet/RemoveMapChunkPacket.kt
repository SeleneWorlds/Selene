package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet

data class RemoveMapChunkPacket(
    val x: Int,
    val y: Int,
    val z: Int,
    val width: Int,
    val height: Int
) : Packet {
    companion object {
        fun decode(buf: ByteBuf): RemoveMapChunkPacket {
            val x = buf.readInt()
            val y = buf.readInt()
            val z = buf.readInt()
            val width = buf.readInt()
            val height = buf.readInt()
            return RemoveMapChunkPacket(x, y, z, width, height)
        }

        fun encode(buf: ByteBuf, packet: RemoveMapChunkPacket) {
            buf.writeInt(packet.x)
            buf.writeInt(packet.y)
            buf.writeInt(packet.z)
            buf.writeInt(packet.width)
            buf.writeInt(packet.height)
        }
    }
}
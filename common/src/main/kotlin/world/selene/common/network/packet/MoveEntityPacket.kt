package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readCoordinate
import world.selene.common.network.writeCoordinate
import world.selene.common.util.Coordinate

data class MoveEntityPacket(
    val networkId: Int,
    val start: Coordinate,
    val end: Coordinate,
    val facing: Float,
    val duration: Float
) : Packet {
    companion object {
        fun decode(buf: ByteBuf): MoveEntityPacket {
            val networkId = buf.readInt()
            val start = buf.readCoordinate()
            val end = buf.readCoordinate()
            val facing = buf.readFloat()
            val duration = buf.readFloat()
            return MoveEntityPacket(networkId, start, end, facing, duration)
        }

        fun encode(buf: ByteBuf, packet: MoveEntityPacket) {
            buf.writeInt(packet.networkId)
            buf.writeCoordinate(packet.start)
            buf.writeCoordinate(packet.end)
            buf.writeFloat(packet.facing)
            buf.writeFloat(packet.duration)
        }
    }
}
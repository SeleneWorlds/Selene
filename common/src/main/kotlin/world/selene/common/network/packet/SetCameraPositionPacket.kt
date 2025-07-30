package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readCoordinate
import world.selene.common.network.writeCoordinate
import world.selene.common.util.Coordinate

data class SetCameraPositionPacket(val coordinate: Coordinate) : Packet {
    companion object {
        fun decode(buf: ByteBuf): SetCameraPositionPacket {
            val position = buf.readCoordinate()
            return SetCameraPositionPacket(position)
        }
        fun encode(buf: ByteBuf, packet: SetCameraPositionPacket) {
            buf.writeCoordinate(packet.coordinate)
        }
    }
}

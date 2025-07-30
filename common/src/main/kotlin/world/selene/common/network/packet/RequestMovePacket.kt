package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readCoordinate
import world.selene.common.network.writeCoordinate
import world.selene.common.util.Coordinate

data class RequestMovePacket(val coordinate: Coordinate): Packet {
    companion object {
        fun decode(buf: ByteBuf): RequestMovePacket {
            val coordinate = buf.readCoordinate()
            return RequestMovePacket(coordinate)
        }

        fun encode(buf: ByteBuf, packet: RequestMovePacket) {
            buf.writeCoordinate(packet.coordinate)
        }
    }
}
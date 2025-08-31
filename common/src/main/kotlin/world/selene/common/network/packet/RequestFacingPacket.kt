package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet

data class RequestFacingPacket(val angle: Float): Packet {
    companion object {
        fun decode(buf: ByteBuf): RequestFacingPacket {
            val angle = buf.readFloat()
            return RequestFacingPacket(angle)
        }

        fun encode(buf: ByteBuf, packet: RequestFacingPacket) {
            buf.writeFloat(packet.angle)
        }
    }
}
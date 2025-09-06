package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet

/**
 * Packet to instruct the client to follow a specific entity with the camera.
 */
data class SetCameraFollowEntityPacket(val networkId: Int) : Packet {
    companion object {
        fun decode(buf: ByteBuf): SetCameraFollowEntityPacket {
            val networkId = buf.readInt()
            return SetCameraFollowEntityPacket(networkId)
        }

        fun encode(buf: ByteBuf, packet: SetCameraFollowEntityPacket) {
            buf.writeInt(packet.networkId)
        }
    }
}

package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readCoordinate
import world.selene.common.network.writeCoordinate
import world.selene.common.util.Coordinate

data class PlaySoundPacket(
    val soundId: Int,
    val volume: Float = 1f,
    val pitch: Float = 1f,
    val coordinate: Coordinate? = null,
) : Packet {

    companion object {
        fun decode(buf: ByteBuf): PlaySoundPacket {
            val soundName = buf.readInt()
            val volume = buf.readFloat()
            val pitch = buf.readFloat()
            val hasPosition = buf.readBoolean()
            val coordinate = if (hasPosition) {
                buf.readCoordinate()
            } else {
                null
            }
            return PlaySoundPacket(soundName, volume, pitch, coordinate)
        }

        fun encode(buf: ByteBuf, packet: PlaySoundPacket) {
            buf.writeInt(packet.soundId)
            buf.writeFloat(packet.volume)
            buf.writeFloat(packet.pitch)
            val hasPosition = packet.coordinate != null
            buf.writeBoolean(hasPosition)
            if (hasPosition) {
                buf.writeCoordinate(packet.coordinate)
            }
        }
    }
}

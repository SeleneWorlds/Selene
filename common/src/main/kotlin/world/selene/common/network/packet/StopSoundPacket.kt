package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readString
import world.selene.common.network.writeString

data class StopSoundPacket(val soundName: String) : Packet {

    companion object {
        fun decode(buf: ByteBuf): StopSoundPacket {
            val soundName = buf.readString()
            return StopSoundPacket(soundName)
        }

        fun encode(buf: ByteBuf, packet: StopSoundPacket) {
            buf.writeString(packet.soundName)
        }
    }
}

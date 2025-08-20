package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readString
import world.selene.common.network.writeString

data class StopSoundPacket(val soundId: Int) : Packet {

    companion object {
        fun decode(buf: ByteBuf): StopSoundPacket {
            val soundId = buf.readInt()
            return StopSoundPacket(soundId)
        }

        fun encode(buf: ByteBuf, packet: StopSoundPacket) {
            buf.writeInt(packet.soundId)
        }
    }
}

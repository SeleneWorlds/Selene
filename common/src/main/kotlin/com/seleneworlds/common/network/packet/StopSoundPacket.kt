package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet

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

package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readString
import world.selene.common.network.writeString

data class PreferencesPacket(val locale: String) : Packet {

    companion object {
        fun decode(buf: ByteBuf): PreferencesPacket {
            val locale = buf.readString()
            return PreferencesPacket(locale)
        }

        fun encode(buf: ByteBuf, packet: PreferencesPacket) {
            buf.writeString(packet.locale)
        }
    }
}
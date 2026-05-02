package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readString
import com.seleneworlds.common.network.writeString

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
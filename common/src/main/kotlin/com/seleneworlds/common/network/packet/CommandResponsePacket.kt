package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readString
import com.seleneworlds.common.network.writeString

data class CommandResponsePacket(val success: Boolean, val response: String) : Packet {

    companion object {
        fun decode(buf: ByteBuf): CommandResponsePacket {
            val success = buf.readBoolean()
            val response = buf.readString()
            return CommandResponsePacket(success, response)
        }

        fun encode(buf: ByteBuf, packet: CommandResponsePacket) {
            buf.writeBoolean(packet.success)
            buf.writeString(packet.response)
        }
    }
}
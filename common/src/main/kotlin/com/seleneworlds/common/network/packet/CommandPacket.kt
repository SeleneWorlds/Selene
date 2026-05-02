package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readString
import com.seleneworlds.common.network.writeString

data class CommandPacket(val command: String) : Packet {

    companion object {
        fun decode(buf: ByteBuf): CommandPacket {
            val command = buf.readString()
            return CommandPacket(command)
        }

        fun encode(buf: ByteBuf, packet: CommandPacket) {
            buf.writeString(packet.command)
        }
    }
}
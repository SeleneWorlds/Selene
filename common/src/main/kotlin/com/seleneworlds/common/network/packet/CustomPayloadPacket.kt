package com.seleneworlds.common.network.packet

import io.netty.buffer.ByteBuf
import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.readString
import com.seleneworlds.common.network.writeString

data class CustomPayloadPacket(val payloadId: String, val payload: String) : Packet {

    companion object {
        fun decode(buf: ByteBuf): CustomPayloadPacket {
            val payloadId = buf.readString()
            val payload = buf.readString()
            return CustomPayloadPacket(payloadId, payload)
        }

        fun encode(buf: ByteBuf, packet: CustomPayloadPacket) {
            buf.writeString(packet.payloadId)
            buf.writeString(packet.payload)
        }
    }
}
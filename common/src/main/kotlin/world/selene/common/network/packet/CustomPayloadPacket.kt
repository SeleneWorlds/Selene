package world.selene.common.network.packet

import io.netty.buffer.ByteBuf
import world.selene.common.network.Packet
import world.selene.common.network.readString
import world.selene.common.network.writeString

data class CustomPayloadPacket(val payloadId: String, val payload: Map<Any, Any>) : Packet {

    companion object {
        fun decode(buf: ByteBuf): CustomPayloadPacket {
            val payloadId = buf.readString()
            val size = buf.readShort().toInt()
            val payload = HashMap<Any, Any>(size)
            for (i in 0 until size) {
                val key = buf.readString()
                val type = buf.readByte().toInt()
                val value = when (type) {
                    1 -> buf.readInt()
                    2 -> buf.readString()
                    3 -> buf.readBoolean()
                    4 -> buf.readFloat()
                    else -> throw IllegalArgumentException("Unknown type: $type")
                }
                payload[key] = value
            }
            return CustomPayloadPacket(payloadId, payload)
        }

        fun encode(buf: ByteBuf, packet: CustomPayloadPacket) {
            buf.writeString(packet.payloadId)
            buf.writeShort(packet.payload.size)
            for ((key, value) in packet.payload) {
                buf.writeString(key.toString())
                when (value) {
                    is Byte -> {
                        buf.writeByte(1)
                        buf.writeInt(value.toInt())
                    }

                    is Short -> {
                        buf.writeByte(1)
                        buf.writeInt(value.toInt())
                    }

                    is Int -> {
                        buf.writeByte(1)
                        buf.writeInt(value)
                    }

                    is Long -> {
                        throw IllegalArgumentException("Long is not supported")
                    }

                    is Float -> {
                        buf.writeByte(4)
                        buf.writeFloat(value)
                    }

                    is Double -> {
                        buf.writeByte(4)
                        buf.writeFloat(value.toFloat())
                    }

                    is String -> {
                        buf.writeByte(2)
                        buf.writeString(value)
                    }

                    is Boolean -> {
                        buf.writeByte(3)
                        buf.writeBoolean(value)
                    }
                }
            }
        }
    }
}
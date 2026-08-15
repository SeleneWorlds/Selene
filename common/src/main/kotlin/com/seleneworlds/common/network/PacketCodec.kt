package com.seleneworlds.common.network

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import java.io.IOException

class PacketCodec(private val factory: PacketFactory) {
    fun read(buf: ByteBuf): Packet? {
        if (buf.readableBytes() == 0) {
            return null
        }

        val packetId = buf.readUnsignedByte().toInt()
        val packet = factory.readPacket(packetId, buf)
            ?: throw IOException("Received an invalid packet id $packetId")

        if (buf.readableBytes() != 0) {
            throw IOException("Unexpected packet size, ${buf.readableBytes()} extra bytes in packet $packet (id: $packetId)")
        }

        return packet
    }

    fun write(allocator: ByteBufAllocator, packet: Packet): ByteBuf {
        val buf = allocator.buffer()
        try {
            factory.writePacket(buf, packet)
            return buf
        } catch (e: Exception) {
            buf.release()
            throw e
        }
    }
}

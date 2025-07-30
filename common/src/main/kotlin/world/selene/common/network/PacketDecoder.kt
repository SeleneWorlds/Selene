package world.selene.common.network

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

import java.io.IOException

class PacketDecoder(private val factory: PacketFactory) : ByteToMessageDecoder() {

    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        if (buf.readableBytes() != 0) {
            val packetId = buf.readUnsignedByte().toInt()
            val payloadSize = buf.readableBytes()
            val packet = factory.readPacket(packetId, buf)
            if (packet != null) {
                // println("Received packet $packet (id: $packetId) with $payloadSize bytes")
                if (buf.readableBytes() == 0) {
                    out.add(packet)
                } else {
                    throw IOException("Unexpected packet size, " + buf.readableBytes() + " extra bytes in packet " + packet + " (id: " + packetId + ")")
                }
            } else {
                throw IOException("Received an invalid packet id $packetId")
            }
        }
    }

}

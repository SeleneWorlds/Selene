package com.seleneworlds.common.network

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class PacketDecoder(factory: PacketFactory) : ByteToMessageDecoder() {
    private val codec = PacketCodec(factory)

    override fun decode(ctx: ChannelHandlerContext, buf: ByteBuf, out: MutableList<Any>) {
        val packet = codec.read(buf)
        if (packet != null) {
            out.add(packet)
        }
    }

}

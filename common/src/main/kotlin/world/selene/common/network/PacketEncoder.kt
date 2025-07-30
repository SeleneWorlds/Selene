package world.selene.common.network

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

@ChannelHandler.Sharable
class PacketEncoder(private val factory: PacketFactory) : MessageToByteEncoder<Packet>() {
    override fun encode(ctx: ChannelHandlerContext, msg: Packet, buf: ByteBuf) {
        factory.writePacket(buf, msg)
    }
}

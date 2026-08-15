package com.seleneworlds.server.network

import com.seleneworlds.common.network.PacketCodec
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame

class WebSocketPacketHandler(
    private val server: NetworkServer,
    private val client: WebSocketNetworkClient,
    private val packetCodec: PacketCodec
) : SimpleChannelInboundHandler<WebSocketFrame>() {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: WebSocketFrame) {
        when (msg) {
            is BinaryWebSocketFrame -> {
                val packet = packetCodec.read(msg.content()) ?: return
                client.receive(packet)
            }

            is TextWebSocketFrame -> {
                ctx.close()
            }

            else -> {
                msg.retain()
                ctx.fireChannelRead(msg)
            }
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        server.reportClientError(client, cause)
    }
}

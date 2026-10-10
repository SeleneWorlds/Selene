package com.seleneworlds.server.network

import com.seleneworlds.common.network.PacketCodec
import com.seleneworlds.common.network.packet.HeartbeatPacket
import io.netty.handler.timeout.IdleStateEvent
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler

class WebSocketPacketHandler(
    private val server: NetworkServer,
    private val client: WebSocketNetworkClient,
    private val packetCodec: PacketCodec
) : SimpleChannelInboundHandler<WebSocketFrame>() {

    private var upgraded = false

    override fun channelRead0(ctx: ChannelHandlerContext, msg: WebSocketFrame) {
        when (msg) {
            is BinaryWebSocketFrame -> {
                val packet = packetCodec.read(msg.content()) ?: return
                if (packet !is HeartbeatPacket) client.receive(packet)
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

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is WebSocketServerProtocolHandler.HandshakeComplete) {
            upgraded = true
            ctx.fireUserEventTriggered(evt)
        } else if (upgraded && evt is IdleStateEvent) {
            client.send(HeartbeatPacket())
        } else {
            ctx.fireUserEventTriggered(evt)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        server.reportClientError(client, cause)
    }
}

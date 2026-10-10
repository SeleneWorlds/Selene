package com.seleneworlds.common.network

import com.seleneworlds.common.network.packet.HeartbeatPacket
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.timeout.IdleStateEvent

class HeartbeatHandler(private val server: Boolean) : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HeartbeatPacket) {
            if (!server) ctx.writeAndFlush(HeartbeatPacket())
        } else {
            ctx.fireChannelRead(msg)
        }
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (server && evt is IdleStateEvent) {
            ctx.writeAndFlush(HeartbeatPacket())
        } else {
            ctx.fireUserEventTriggered(evt)
        }
    }
}

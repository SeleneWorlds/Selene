package com.seleneworlds.server.network

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.socket.SocketChannel
import io.netty.util.AttributeKey
import com.seleneworlds.common.network.Packet
import com.seleneworlds.server.players.PlayerManager
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue

class NetworkClientImpl(
    private val server: NetworkServer,
    playerManager: PlayerManager,
    private val channel: SocketChannel
) : ChannelInboundHandlerAdapter(), ChannelFutureListener, NetworkClient {

    val player = playerManager.createPlayer(this)
    private val incomingPackets = ConcurrentLinkedQueue<Packet>()

    override fun poll(): Packet? = incomingPackets.poll()

    override fun enqueueWork(runnable: Runnable) {
        // TODO Currently just runs immediately, but process runs on the MainThread too.
        //      We should start running some things off-thread and be more explicit about when we want to run on main, at which point this needs to be implemented properly
        runnable.run()
    }

    override fun send(packet: Packet) {
        channel.writeAndFlush(packet).addListener(this)
    }

    override fun disconnect() {
        channel.disconnect().addListener {
            channel.close()
        }
    }

    override val address: InetSocketAddress get() = channel.remoteAddress() as InetSocketAddress

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        incomingPackets.add(msg as Packet)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable) {
        server.reportClientError(this, cause)
    }

    override fun operationComplete(future: ChannelFuture) {
        if (!future.isSuccess) {
            server.reportClientError(this, future.cause())
        }
    }

    companion object {
        val ATTRIBUTE: AttributeKey<NetworkClientImpl> = AttributeKey.valueOf("selene:client")
    }

}
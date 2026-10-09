package com.seleneworlds.server.network

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.socket.SocketChannel
import com.seleneworlds.common.network.Packet
import com.seleneworlds.server.config.PacketRateLimit
import kotlin.reflect.KClass
import com.seleneworlds.server.players.PlayerManager
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

class NetworkClientImpl(
    private val server: NetworkServer,
    playerManager: PlayerManager,
    private val channel: SocketChannel,
    maxQueuedPackets: Int,
    packetRateLimits: Map<KClass<out Packet>, PacketRateLimit> = emptyMap()
) : ChannelInboundHandlerAdapter(), ChannelFutureListener, NetworkPlayerClient {

    override val player = playerManager.createPlayer(this)
    private val incomingPackets = IncomingPacketQueue(maxQueuedPackets, PacketRateLimiter(packetRateLimits))
    private val disconnecting = AtomicBoolean()
    private val queueOverflowReported = AtomicBoolean()

    override fun poll(): Packet? = incomingPackets.poll()

    override fun enqueueWork(runnable: Runnable) {
        // TODO Currently just runs immediately, but process runs on the MainThread too.
        //      We should start running some things off-thread and be more explicit about when we want to run on main, at which point this needs to be implemented properly
        runnable.run()
    }

    override val writable: Boolean get() = channel.isActive && channel.isWritable

    override fun send(packet: Packet) {
        channel.writeAndFlush(packet).addListener(this)
    }

    override fun disconnect() {
        if (!disconnecting.compareAndSet(false, true)) return
        channel.disconnect().addListener {
            channel.close()
        }
    }

    override val address: InetSocketAddress get() = channel.remoteAddress() as InetSocketAddress

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (!incomingPackets.offer(msg as Packet) && queueOverflowReported.compareAndSet(false, true)) {
            server.reportClientError(this, IllegalStateException("Incoming packet queue limit exceeded"))
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable) {
        server.reportClientError(this, cause)
    }

    override fun operationComplete(future: ChannelFuture) {
        if (!future.isSuccess) {
            server.reportClientError(this, future.cause())
        }
    }
}

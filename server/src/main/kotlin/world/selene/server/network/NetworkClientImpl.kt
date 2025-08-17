package world.selene.server.network

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.socket.SocketChannel
import io.netty.util.AttributeKey
import world.selene.common.network.Packet
import world.selene.server.player.PlayerManager
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue

class NetworkClientImpl(
    private val server: NetworkServer,
    private val playerManager: PlayerManager,
    private val channel: SocketChannel
) : ChannelInboundHandlerAdapter(), ChannelFutureListener, NetworkClient {

    val player = playerManager.createPlayer(this)
    private val incomingPackets = ConcurrentLinkedQueue<Packet>()

    override fun poll(): Packet? = incomingPackets.poll()

    override fun enqueueWork(runnable: Runnable) {
        // TODO Currently just runs immediately.
        runnable.run()
    }

    override fun send(packet: Packet) {
        channel.writeAndFlush(packet).addListener(this)
    }

    override fun disconnect() {
        channel.close()
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
package com.seleneworlds.server.network

import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.PacketCodec
import com.seleneworlds.server.players.PlayerManager
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue

class WebSocketNetworkClient(
    private val server: NetworkServer,
    playerManager: PlayerManager,
    private val channel: Channel,
    private val packetCodec: PacketCodec
) : ChannelFutureListener, NetworkClient {

    val player = playerManager.createPlayer(this)
    private val incomingPackets = ConcurrentLinkedQueue<Packet>()

    override fun poll(): Packet? = incomingPackets.poll()

    override fun enqueueWork(runnable: Runnable) {
        runnable.run()
    }

    override fun send(packet: Packet) {
        val payload = packetCodec.write(channel.alloc(), packet)
        channel.writeAndFlush(BinaryWebSocketFrame(payload)).addListener(this)
    }

    override fun disconnect() {
        channel.disconnect().addListener {
            channel.close()
        }
    }

    override val address: InetSocketAddress
        get() = channel.remoteAddress() as InetSocketAddress

    fun receive(packet: Packet) {
        incomingPackets.add(packet)
    }

    override fun operationComplete(future: ChannelFuture) {
        if (!future.isSuccess) {
            server.reportClientError(this, future.cause())
        }
    }
}

package com.seleneworlds.server.network

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.timeout.ReadTimeoutHandler
import org.slf4j.Logger
import com.seleneworlds.common.network.PacketCodec
import com.seleneworlds.common.network.PacketDecoder
import com.seleneworlds.common.network.PacketEncoder
import com.seleneworlds.common.network.PacketFactory
import com.seleneworlds.common.network.PacketHandler
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.server.players.PlayerManager
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

@ChannelHandler.Sharable
class NetworkServerImpl(
    private val packetFactory: PacketFactory,
    private val packetHandler: PacketHandler<NetworkClient>,
    private val playerManager: PlayerManager,
    private val config: ServerConfig,
    private val logger: Logger
) : ChannelInboundHandlerAdapter(), NetworkServer {

    private val networkReadTimeout: Int = 60000
    private val maxFrameLength = Short.MAX_VALUE.toInt()
    private val lengthFieldLength = 2
    private val webSocketPath = "/ws"
    private val packetCodec = PacketCodec(packetFactory)

    private val bossGroup: EventLoopGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
    private val workerGroup: EventLoopGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
    private var tcpChannel: Channel? = null
    private var webSocketChannel: Channel? = null

    override val clients = ConcurrentLinkedQueue<NetworkClient>()

    override fun start(port: Int) {
        startTcp(port)
        if (config.webSocketPort > 0) {
            startWebSocket(config.webSocketPort)
        }
    }

    private fun startTcp(port: Int) {
        val bootstrap = ServerBootstrap()
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(channel: SocketChannel) {
                    val client = NetworkClientImpl(this@NetworkServerImpl, playerManager, channel)
                    channel.attr(NetworkClientAttributes.CLIENT).set(client)
                    clients.add(client)
                    channel.pipeline()
                        .addLast("timeout", ReadTimeoutHandler(networkReadTimeout))
                        .addLast("server", this@NetworkServerImpl)
                        .addLast(
                            "frameDecoder",
                            LengthFieldBasedFrameDecoder(
                                maxFrameLength,
                                0,
                                lengthFieldLength,
                                0,
                                lengthFieldLength,
                                true
                            )
                        )
                        .addLast("decoder", PacketDecoder(packetFactory))
                        .addLast("packetHandler", client)
                        .addLast("framePrepender", LengthFieldPrepender(lengthFieldLength, 0, false))
                        .addLast("encoder", PacketEncoder(packetFactory))
                }
            })
            .option(ChannelOption.SO_BACKLOG, 128)

        bootstrap.bind(port).addListener { future ->
            if (future.isSuccess) {
                tcpChannel = (future as ChannelFuture).channel()
                logger.info("TCP game server is listening on port $port")
            } else {
                logger.error("Failed to start TCP game server on port $port", future.cause())
            }
        }
    }

    private fun startWebSocket(port: Int) {
        val bootstrap = ServerBootstrap()
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(channel: SocketChannel) {
                    val client = WebSocketNetworkClient(this@NetworkServerImpl, playerManager, channel, packetCodec)
                    channel.attr(NetworkClientAttributes.CLIENT).set(client)
                    clients.add(client)
                    channel.pipeline()
                        .addLast("timeout", ReadTimeoutHandler(networkReadTimeout))
                        .addLast("server", this@NetworkServerImpl)
                        .addLast("httpCodec", HttpServerCodec())
                        .addLast("httpAggregator", HttpObjectAggregator(maxFrameLength))
                        .addLast(
                            "webSocketProtocol",
                            WebSocketServerProtocolHandler(webSocketPath, null, true, maxFrameLength)
                        )
                        .addLast("packetHandler", WebSocketPacketHandler(this@NetworkServerImpl, client, packetCodec))
                }
            })
            .option(ChannelOption.SO_BACKLOG, 128)

        bootstrap.bind(port).addListener { future ->
            if (future.isSuccess) {
                webSocketChannel = (future as ChannelFuture).channel()
                logger.info("WebSocket game server is listening on port $port at $webSocketPath")
            } else {
                logger.error("Failed to start WebSocket game server on port $port", future.cause())
            }
        }
    }

    override fun process() {
        clients.forEach { client ->
            var packet = client.poll()
            while (packet != null) {
                packetHandler.handle(client, packet)
                packet = client.poll()
            }

            (client as NetworkClientImpl).player.update()
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)
        val client = ctx.channel().attr(NetworkClientAttributes.CLIENT).get()
            ?: return
        clients.remove(client)
        playerManager.removePlayer(client)
        logger.info("Client disconnected: ${client.address}")
    }

    override fun stop() {
        tcpChannel?.close()
        webSocketChannel?.close()
        workerGroup.shutdownGracefully().awaitUninterruptibly(5, TimeUnit.SECONDS)
        bossGroup.shutdownGracefully().awaitUninterruptibly(5, TimeUnit.SECONDS)
    }

    override fun reportClientError(client: NetworkClient, cause: Throwable) {
        logger.error("Client error: ${client.address}", cause)
        client.disconnect()
    }

}

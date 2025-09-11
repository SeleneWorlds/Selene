package world.selene.server.network

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.timeout.ReadTimeoutHandler
import org.slf4j.Logger
import world.selene.common.network.PacketDecoder
import world.selene.common.network.PacketEncoder
import world.selene.common.network.PacketFactory
import world.selene.common.network.PacketHandler
import world.selene.server.players.PlayerManager
import java.util.concurrent.ConcurrentLinkedQueue

@ChannelHandler.Sharable
class NetworkServerImpl(
    private val packetFactory: PacketFactory,
    private val packetHandler: PacketHandler<NetworkClient>,
    private val playerManager: PlayerManager,
    private val logger: Logger
) : ChannelInboundHandlerAdapter(), NetworkServer {

    private val networkReadTimeout: Int = 60000
    private val maxFrameLength = Short.MAX_VALUE.toInt()
    private val lengthFieldLength = 2

    private val bossGroup: EventLoopGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
    private val workerGroup: EventLoopGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
    private var channel: Channel? = null

    override val clients = ConcurrentLinkedQueue<NetworkClient>()

    override fun start(port: Int) {
        val bootstrap = ServerBootstrap()
        bootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(channel: SocketChannel) {
                    val client = NetworkClientImpl(this@NetworkServerImpl, playerManager, channel)
                    channel.attr(NetworkClientImpl.ATTRIBUTE).set(client)
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

        bootstrap.bind(port).addListener {
            if (it.isSuccess) {
                logger.info("Server is listening on port $port")
            } else {
                logger.error("Failed to start server on port $port", it.cause())
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
        val client = ctx.channel().attr(NetworkClientImpl.ATTRIBUTE).get()
        clients.remove(client)
        playerManager.removePlayer(client)
        logger.info("Client disconnected: ${client.address}")
    }

    override fun stop() {
        channel?.close()
        workerGroup.shutdownGracefully()
        bossGroup.shutdownGracefully()
    }

    override fun reportClientError(client: NetworkClient, cause: Throwable) {
        logger.error("Client error: ${client.address}", cause)
        client.disconnect()
    }

}

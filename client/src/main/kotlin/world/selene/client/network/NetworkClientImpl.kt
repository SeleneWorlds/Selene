package world.selene.client.network

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.local.LocalChannel
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.LengthFieldPrepender
import io.netty.handler.timeout.ReadTimeoutHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import world.selene.common.network.Packet
import world.selene.common.network.PacketDecoder
import world.selene.common.network.PacketEncoder
import world.selene.common.network.PacketFactory
import world.selene.common.network.PacketHandler
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentLinkedQueue

@Sharable
class NetworkClientImpl(
    private val packetFactory: PacketFactory,
    private val logger: Logger
) : ChannelInboundHandlerAdapter(), ChannelFutureListener, NetworkClient {

    var packetHandler: PacketHandler<NetworkClient>? = null

    private val networkReadTimeout: Int = 60000
    private val maxFrameLength = Short.MAX_VALUE.toInt()
    private val lengthFieldLength = 2

    private val incomingPackets = ConcurrentLinkedQueue<Packet>()
    private var packetsSentTotal = 0
    private var packetsReceivedTotal = 0
    private var timeSinceLastSecond = 0
    private var packetsSentThisSecond = 0
    private var packetsSentPerSecond = 0f
    private var packetsReceivedThisSecond = 0
    private var packetsReceivedPerSecond = 0f

    private val workerGroup: EventLoopGroup = MultiThreadIoEventLoopGroup(NioIoHandler.newFactory())
    private val bootstrap = Bootstrap()
        .group(workerGroup)
        .channel(NioSocketChannel::class.java)
        .option(ChannelOption.SO_KEEPALIVE, true)
        .handler(object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel) {
                ch.pipeline()
                    .addLast("timeout", ReadTimeoutHandler(networkReadTimeout))
                    .addLast(
                        "frameDecoder",
                        LengthFieldBasedFrameDecoder(maxFrameLength, 0, lengthFieldLength, 0, lengthFieldLength)
                    )
                    .addLast("decoder", PacketDecoder(packetFactory))
                    .addLast("packetHandler", this@NetworkClientImpl)
                    .addLast("framePrepender", LengthFieldPrepender(lengthFieldLength))
                    .addLast("encoder", PacketEncoder(packetFactory))
            }
        })

    var channel: Channel = LocalChannel()

    private val workQueue = ConcurrentLinkedQueue<Runnable>()

    override val address: InetSocketAddress get() = channel.remoteAddress() as InetSocketAddress
    override val connected: Boolean get() = channel.isRegistered && channel.isActive

    override suspend fun connect(host: String, port: Int) {
        withContext(Dispatchers.IO) {
            bootstrap.connect(host, port).addListener {
                channel = (it as ChannelFuture).channel()

                logger.info("Connected to $host:$port")
                Thread({
                    var last = System.currentTimeMillis()
                    while (!workerGroup.isShutdown) {
                        val now = System.currentTimeMillis()
                        val delta = (now - last).toInt()
                        last = now

                        processPackets(delta)

                        Thread.sleep(10)
                    }
                }, "NetworkClient").start()
            }.get()
        }
    }

    override fun send(packet: Packet) {
        packetsSentTotal++
        packetsSentThisSecond++
        channel.writeAndFlush(packet).addListener(this)
    }

    override fun disconnect() {
        channel.close()
        workerGroup.shutdownGracefully()
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        packetsReceivedTotal++
        packetsReceivedThisSecond++
        incomingPackets.add(msg as Packet)
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)
        logger.info("Disconnected from server")
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable) {
        logger.error("Network error", cause)
        disconnect()
    }

    override fun operationComplete(future: ChannelFuture) {
        if (!future.isSuccess) {
            exceptionCaught(null, future.cause())
        }
    }

    private fun updateStats(delta: Int) {
        timeSinceLastSecond += delta
        if (timeSinceLastSecond >= 1000) {
            timeSinceLastSecond = 0
            packetsSentPerSecond = (packetsSentPerSecond * 0.9f) + (packetsSentThisSecond * 0.1f)
            packetsReceivedPerSecond = (packetsReceivedPerSecond * 0.9f) + (packetsReceivedThisSecond * 0.1f)
            packetsSentThisSecond = 0
            packetsReceivedThisSecond = 0
        }
    }

    private fun processPackets(delta: Int) {
        updateStats(delta)

        val packetHandler = packetHandler ?: return
        var packet = incomingPackets.poll()
        while (packet != null) {
            packetHandler.handle(this, packet)
            packet = incomingPackets.poll()
        }
    }

    override fun enqueueWork(runnable: Runnable) {
        workQueue.add(runnable)
    }

    override fun processWork() {
        var task = workQueue.poll()
        while (task != null) {
            try {
                task.run()
            } catch (e: Exception) {
                logger.error("Exception during work execution", e)
            }
            task = workQueue.poll()
        }
    }
}
package world.selene.client.network

import world.selene.common.network.Packet
import java.net.InetSocketAddress

interface NetworkClient {
    val address: InetSocketAddress
    val connected: Boolean
    suspend fun connect(host: String, port: Int)
    fun send(packet: Packet)
    fun disconnect()
    fun enqueueWork(runnable: Runnable)
    fun processWork()
}

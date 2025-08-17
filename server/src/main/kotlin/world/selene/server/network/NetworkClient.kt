package world.selene.server.network

import world.selene.common.network.Packet
import java.net.InetSocketAddress

interface NetworkClient {
    fun send(packet: Packet)
    fun disconnect()
    val address: InetSocketAddress
    fun poll(): Packet?
    fun enqueueWork(runnable: Runnable)
}
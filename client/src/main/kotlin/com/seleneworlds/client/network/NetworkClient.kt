package com.seleneworlds.client.network

import io.netty.channel.ChannelFuture
import com.seleneworlds.common.network.Packet
import java.net.InetSocketAddress

interface NetworkClient {
    val address: InetSocketAddress
    val connected: Boolean
    fun connect(host: String, port: Int): ChannelFuture
    fun send(packet: Packet)
    fun disconnect()
    fun enqueueWork(runnable: Runnable)
    fun processWork()
}

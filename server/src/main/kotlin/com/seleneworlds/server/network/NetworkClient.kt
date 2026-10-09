package com.seleneworlds.server.network

import com.seleneworlds.common.network.Packet
import java.net.InetSocketAddress

interface NetworkClient {
    val writable: Boolean
    fun send(packet: Packet)
    fun disconnect()
    val address: InetSocketAddress
    fun poll(): Packet?
    fun enqueueWork(runnable: Runnable)
}
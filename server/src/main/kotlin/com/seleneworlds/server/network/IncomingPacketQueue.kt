package com.seleneworlds.server.network

import com.seleneworlds.common.network.Packet
import java.util.concurrent.ArrayBlockingQueue

internal class IncomingPacketQueue(capacity: Int) {
    private val packets = ArrayBlockingQueue<Packet>(capacity)

    fun offer(packet: Packet): Boolean = packets.offer(packet)

    fun poll(): Packet? = packets.poll()
}

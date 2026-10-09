package com.seleneworlds.server.network

import com.seleneworlds.common.network.Packet
import java.util.concurrent.ArrayBlockingQueue

internal class IncomingPacketQueue(
    capacity: Int,
    private val rateLimiter: PacketRateLimiter = PacketRateLimiter(emptyMap())
) {
    private val packets = ArrayBlockingQueue<Packet>(capacity)

    // A throttled packet is deliberately discarded; only queue overflow returns false.
    fun offer(packet: Packet): Boolean = !rateLimiter.allow(packet) || packets.offer(packet)

    fun poll(): Packet? = packets.poll()
}

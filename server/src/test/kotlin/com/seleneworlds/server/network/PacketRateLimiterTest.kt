package com.seleneworlds.server.network

import com.seleneworlds.common.network.Packet
import com.seleneworlds.server.config.PacketRateLimit
import kotlin.test.*

class PacketRateLimiterTest {
    private class LimitedPacket : Packet
    private class OtherPacket : Packet

    @Test
    fun `allows burst then refills continuously up to capacity`() {
        var now = 0L
        val limiter = PacketRateLimiter(mapOf(LimitedPacket::class to PacketRateLimit(2.0, 2))) { now }
        assertTrue(limiter.allow(LimitedPacket()))
        assertTrue(limiter.allow(LimitedPacket()))
        assertFalse(limiter.allow(LimitedPacket()))
        now = 250_000_000L
        assertFalse(limiter.allow(LimitedPacket()))
        now = 500_000_000L
        assertTrue(limiter.allow(LimitedPacket()))
        assertFalse(limiter.allow(LimitedPacket()))
        now = 100_000_000_000L
        assertTrue(limiter.allow(LimitedPacket()))
        assertTrue(limiter.allow(LimitedPacket()))
        assertFalse(limiter.allow(LimitedPacket()))
    }

    @Test
    fun `connections and packet types have independent budgets`() {
        val rules = mapOf(LimitedPacket::class to PacketRateLimit(1.0, 1), OtherPacket::class to PacketRateLimit(1.0, 1))
        val first = PacketRateLimiter(rules) { 0L }
        val second = PacketRateLimiter(rules) { 0L }
        assertTrue(first.allow(LimitedPacket()))
        assertFalse(first.allow(LimitedPacket()))
        assertTrue(first.allow(OtherPacket()))
        assertTrue(second.allow(LimitedPacket()))
    }

    @Test
    fun `unconfigured packets are unrestricted`() {
        val limiter = PacketRateLimiter(mapOf(LimitedPacket::class to PacketRateLimit(1.0, 1))) { 0L }
        repeat(1000) { assertTrue(limiter.allow(OtherPacket())) }
    }

    @Test
    fun `throttled packets do not occupy queue space or cause overflow`() {
        val limiter = PacketRateLimiter(mapOf(LimitedPacket::class to PacketRateLimit(1.0, 1))) { 0L }
        val queue = IncomingPacketQueue(1, limiter)
        val packet = LimitedPacket()
        assertTrue(queue.offer(packet))
        repeat(100) { assertTrue(queue.offer(LimitedPacket())) }
        assertFalse(queue.offer(OtherPacket()))
        assertSame(packet, queue.poll())
        assertNull(queue.poll())
    }

    @Test
    fun `rejects invalid rules`() {
        for (rate in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { PacketRateLimit(rate, 1) }
        }
        assertFailsWith<IllegalArgumentException> { PacketRateLimit(1.0, 0) }
        assertFailsWith<IllegalArgumentException> { PacketRateLimit(1.0, -1) }
    }
}

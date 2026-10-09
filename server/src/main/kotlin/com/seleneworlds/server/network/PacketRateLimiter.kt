package com.seleneworlds.server.network

import com.seleneworlds.common.network.Packet
import com.seleneworlds.server.config.PacketRateLimit
import kotlin.reflect.KClass

/** One instance per connection. Called before queue admission, after packet decoding. */
internal class PacketRateLimiter(
    private val rules: Map<KClass<out Packet>, PacketRateLimit>,
    private val clock: () -> Long = System::nanoTime
) {
    private class Bucket(var tokens: Double, var lastRefill: Long)
    private val buckets = mutableMapOf<KClass<out Packet>, Bucket>()

    @Synchronized
    fun allow(packet: Packet): Boolean {
        val rule = rules[packet::class] ?: return true
        val now = clock()
        val bucket = buckets.getOrPut(packet::class) { Bucket(rule.burst.toDouble(), now) }
        val elapsed = now - bucket.lastRefill
        if (elapsed > 0) {
            bucket.tokens = (bucket.tokens + elapsed / 1_000_000_000.0 * rule.packetsPerSecond)
                .coerceAtMost(rule.burst.toDouble())
            bucket.lastRefill = now
        }
        if (bucket.tokens < 1.0) return false
        bucket.tokens -= 1.0
        return true
    }
}

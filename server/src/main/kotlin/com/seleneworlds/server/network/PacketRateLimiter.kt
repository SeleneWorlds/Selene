package com.seleneworlds.server.network

import com.seleneworlds.common.network.Packet
import com.seleneworlds.common.network.packet.CustomPayloadPacket
import com.seleneworlds.server.config.PacketRateLimit
import kotlin.reflect.KClass

/** One instance per connection. Called before queue admission, after packet decoding. */
internal class PacketRateLimiter(
    private val rules: Map<KClass<out Packet>, PacketRateLimit>,
    private val payloadRules: Map<String, PacketRateLimit> = emptyMap(),
    private val payloadDefault: (String) -> PacketRateLimit? = { null },
    private val clock: () -> Long = System::nanoTime
) {
    private class Bucket(var tokens: Double, var lastRefill: Long)
    private val buckets = mutableMapOf<KClass<out Packet>, Bucket>()
    private val payloadBuckets = mutableMapOf<String, Bucket>()
    private val appliedPayloadRules = mutableMapOf<String, PacketRateLimit>()

    @Synchronized
    fun allow(packet: Packet): Boolean {
        val now = clock()
        rules[packet::class]?.let { rule ->
            val bucket = buckets.getOrPut(packet::class) { Bucket(rule.burst.toDouble(), now) }
            // The overall packet budget counts attempts, including payload-specific rejections.
            if (!consume(bucket, rule, now)) return false
        }
        if (packet is CustomPayloadPacket) {
            val rule = payloadRules[packet.payloadId] ?: payloadDefault(packet.payloadId)
            if (rule != null) {
                if (appliedPayloadRules.put(packet.payloadId, rule) != rule) {
                    payloadBuckets.remove(packet.payloadId)
                }
                val bucket = payloadBuckets.getOrPut(packet.payloadId) { Bucket(rule.burst.toDouble(), now) }
                if (!consume(bucket, rule, now)) return false
            } else {
                appliedPayloadRules.remove(packet.payloadId)
                payloadBuckets.remove(packet.payloadId)
            }
        }
        return true
    }

    private fun consume(bucket: Bucket, rule: PacketRateLimit, now: Long): Boolean {
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

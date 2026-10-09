package com.seleneworlds.server.network

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.network.packet.CustomPayloadPacket
import com.seleneworlds.server.config.PacketRateLimit
import java.io.File
import kotlin.test.*

class PayloadRateLimitRegistryTest {
    @Test
    fun `bundle cleanup restores previous defaults and removes owned defaults`() {
        val registry = PayloadRateLimitRegistry()
        val first = Bundle(BundleManifest(name = "first"), File("first"))
        val second = Bundle(BundleManifest(name = "second"), File("second"))
        val slow = PacketRateLimit(1.0, 1)
        val fast = PacketRateLimit(5.0, 5)
        BundleExecutionContext.withBundle(first) { registry.setDefault("example:chat", slow) }
        BundleExecutionContext.withBundle(second) { registry.setDefault("example:chat", fast) }
        assertEquals(fast, registry.getDefault("example:chat"))
        registry.clearBundleState(second)
        assertEquals(slow, registry.getDefault("example:chat"))
        registry.clearBundleState(first)
        assertNull(registry.getDefault("example:chat"))
    }

    @Test
    fun `existing connections observe default changes and removal`() {
        val registry = PayloadRateLimitRegistry()
        val bundle = Bundle(BundleManifest(name = "example"), File("example"))
        val limiter = PacketRateLimiter(emptyMap(), payloadDefault = registry::getDefault) { 0L }
        val packet = CustomPayloadPacket("example:chat", "{}")
        assertTrue(limiter.allow(packet))
        BundleExecutionContext.withBundle(bundle) { registry.setDefault(packet.payloadId, PacketRateLimit(1.0, 1)) }
        assertTrue(limiter.allow(packet))
        assertFalse(limiter.allow(packet))
        BundleExecutionContext.withBundle(bundle) { registry.setDefault(packet.payloadId, PacketRateLimit(2.0, 2)) }
        assertTrue(limiter.allow(packet))
        assertTrue(limiter.allow(packet))
        assertFalse(limiter.allow(packet))
        registry.clearBundleState(bundle)
        repeat(10) { assertTrue(limiter.allow(packet)) }
    }

    @Test
    fun `admin overrides take priority and remain unaffected by default changes`() {
        val registry = PayloadRateLimitRegistry()
        registry.setDefault("example:chat", PacketRateLimit(1.0, 1))
        val limiter = PacketRateLimiter(
            emptyMap(), mapOf("example:chat" to PacketRateLimit(5.0, 2)), registry::getDefault
        ) { 0L }
        val packet = CustomPayloadPacket("example:chat", "{}")
        assertTrue(limiter.allow(packet))
        assertTrue(limiter.allow(packet))
        assertFalse(limiter.allow(packet))
        registry.setDefault(packet.payloadId, PacketRateLimit(100.0, 100))
        assertFalse(limiter.allow(packet))
    }
}

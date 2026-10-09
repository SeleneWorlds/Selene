package com.seleneworlds.server.network

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.bundles.BundleStateCleaner
import com.seleneworlds.server.config.PacketRateLimit

/** Bundle-owned defaults. The most recently registered default wins until its bundle unloads. */
class PayloadRateLimitRegistry : BundleStateCleaner {
    private val registrations = mutableMapOf<String, LinkedHashMap<Bundle?, PacketRateLimit>>()
    @Volatile private var defaults: Map<String, PacketRateLimit> = emptyMap()

    @Synchronized
    fun setDefault(payloadId: String, rule: PacketRateLimit) {
        require(payloadId.isNotBlank()) { "Payload ID must not be blank" }
        val owner = BundleExecutionContext.currentBundle
        val entries = registrations.getOrPut(payloadId) { linkedMapOf() }
        entries.remove(owner)
        entries[owner] = rule
        publish()
    }

    fun getDefault(payloadId: String): PacketRateLimit? = defaults[payloadId]

    @Synchronized
    override fun clearBundleState(bundle: Bundle) {
        registrations.values.forEach { it.remove(bundle) }
        registrations.entries.removeIf { it.value.isEmpty() }
        publish()
    }

    private fun publish() {
        defaults = registrations.mapValues { it.value.values.last() }
    }
}

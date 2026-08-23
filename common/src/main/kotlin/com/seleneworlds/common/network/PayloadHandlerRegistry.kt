package com.seleneworlds.common.network

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.SetMultimap
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleExecutionContext
import com.seleneworlds.common.bundles.BundleStateCleaner
import com.seleneworlds.common.serialization.SerializedMap

private typealias PayloadHandler<T> = (T, SerializedMap) -> Unit

class PayloadHandlerRegistry<T> : BundleStateCleaner {

    private class Registration<T>(
        val bundle: Bundle,
        val payloadId: String,
        val callback: PayloadHandler<T>
    )

    /**
     * Payload handlers may be registered from CEF/AWT threads, so we must lock registrations.
     * Reads go through the volatile dispatchHandlers which acts as a copy-on-write multiplexer.
     */
    private val lock = Any()
    private val payloadHandlers: SetMultimap<String, PayloadHandler<T>> = LinkedHashMultimap.create()
    private val bundleRegistrations = mutableMapOf<Bundle, MutableSet<Registration<T>>>()

    @Volatile
    private var dispatchHandlers: Map<String, List<PayloadHandler<T>>> = emptyMap()

    fun registerHandler(payloadId: String, callback: PayloadHandler<T>): () -> Unit {
        val bundle = BundleExecutionContext.currentBundle
        val registration = bundle?.let { Registration(it, payloadId, callback) }
        synchronized(lock) {
            payloadHandlers.put(payloadId, callback)
            if (registration != null) {
                bundleRegistrations.getOrPut(bundle) { mutableSetOf() }.add(registration)
            }
            updateDispatcher(payloadId)
        }
        return {
            synchronized(lock) {
                payloadHandlers.remove(payloadId, callback)
                if (registration != null) {
                    bundleRegistrations[registration.bundle]?.let { registrations ->
                        registrations.remove(registration)
                        if (registrations.isEmpty()) {
                            bundleRegistrations.remove(registration.bundle)
                        }
                    }
                }
                updateDispatcher(payloadId)
            }
        }
    }

    fun dispatch(payloadId: String, context: T, payload: SerializedMap): Boolean {
        val handlers = dispatchHandlers[payloadId] ?: return false
        handlers.forEach { it(context, payload) }
        return true
    }

    fun hasHandlers(payloadId: String): Boolean = dispatchHandlers.containsKey(payloadId)

    override fun clearBundleState(bundle: Bundle) {
        synchronized(lock) {
            val registrations = bundleRegistrations.remove(bundle) ?: return
            registrations.forEach { registration ->
                payloadHandlers.remove(registration.payloadId, registration.callback)
            }
            registrations.mapTo(mutableSetOf()) { it.payloadId }.forEach(::updateDispatcher)
        }
    }

    private fun updateDispatcher(payloadId: String) {
        val updated = dispatchHandlers.toMutableMap()
        val handlers = payloadHandlers[payloadId]
        if (handlers.isEmpty()) {
            updated.remove(payloadId)
        } else {
            updated[payloadId] = handlers.toList()
        }
        dispatchHandlers = updated
    }

}

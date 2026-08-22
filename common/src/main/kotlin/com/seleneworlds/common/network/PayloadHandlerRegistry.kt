package com.seleneworlds.common.network

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.SetMultimap
import com.seleneworlds.common.serialization.SerializedMap

private typealias PayloadHandler<T> = (T, SerializedMap) -> Unit

class PayloadHandlerRegistry<T> {

    /**
     * Payload handlers may be registered from CEF/AWT threads, so we must lock registrations.
     * Reads go through the volatile dispatchHandlers which acts as a copy-on-write multiplexer.
     */
    private val lock = Any()
    private val payloadHandlers: SetMultimap<String, PayloadHandler<T>> = LinkedHashMultimap.create()

    @Volatile
    private var dispatchHandlers: Map<String, List<PayloadHandler<T>>> = emptyMap()

    fun registerHandler(payloadId: String, callback: PayloadHandler<T>): () -> Unit {
        synchronized(lock) {
            payloadHandlers.put(payloadId, callback)
            updateDispatcher(payloadId)
        }
        return {
            synchronized(lock) {
                payloadHandlers.remove(payloadId, callback)
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

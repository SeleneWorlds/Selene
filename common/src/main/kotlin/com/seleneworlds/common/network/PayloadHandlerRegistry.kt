package com.seleneworlds.common.network

import com.seleneworlds.common.serialization.SerializedMap

class PayloadHandlerRegistry<T> {

    private val lock = Any()
    private val payloadHandlers = mutableMapOf<String, LinkedHashSet<(T, SerializedMap) -> Unit>>()

    fun registerHandler(payloadId: String, callback: (T, SerializedMap) -> Unit): () -> Unit {
        synchronized(lock) { payloadHandlers.getOrPut(payloadId, ::LinkedHashSet).add(callback) }
        return {
            synchronized(lock) {
                payloadHandlers[payloadId]?.let { handlers ->
                    handlers.remove(callback)
                    if (handlers.isEmpty()) payloadHandlers.remove(payloadId)
                }
            }
        }
    }

    fun getHandler(payloadId: String): ((T, SerializedMap) -> Unit)? {
        synchronized(lock) { if (payloadHandlers[payloadId].isNullOrEmpty()) return null }
        return { context, payload ->
            val handlers = synchronized(lock) { payloadHandlers[payloadId]?.toList().orEmpty() }
            handlers.forEach { it(context, payload) }
        }
    }

}

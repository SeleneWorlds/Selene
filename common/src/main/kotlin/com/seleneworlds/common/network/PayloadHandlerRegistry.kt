package com.seleneworlds.common.network

class PayloadHandlerRegistry<T> {

    private val payloadHandlers = mutableMapOf<String, (T, Map<*, *>) -> Unit>()

    fun registerHandler(payloadId: String, callback: (T, Map<*, *>) -> Unit) {
        payloadHandlers[payloadId] = callback
    }

    fun getHandler(payloadId: String): ((T, Map<*, *>) -> Unit)? {
        return payloadHandlers[payloadId]
    }

}
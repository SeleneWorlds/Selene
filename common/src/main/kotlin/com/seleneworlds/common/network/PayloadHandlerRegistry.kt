package com.seleneworlds.common.network

import com.seleneworlds.common.serialization.SerializedMap

class PayloadHandlerRegistry<T> {

    private val payloadHandlers = mutableMapOf<String, (T, SerializedMap) -> Unit>()

    fun registerHandler(payloadId: String, callback: (T, SerializedMap) -> Unit) {
        payloadHandlers[payloadId] = callback
    }

    fun getHandler(payloadId: String): ((T, SerializedMap) -> Unit)? {
        return payloadHandlers[payloadId]
    }

}

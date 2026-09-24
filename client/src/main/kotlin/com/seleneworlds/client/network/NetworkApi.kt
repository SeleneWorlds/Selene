package com.seleneworlds.client.network

import kotlinx.serialization.json.Json
import com.seleneworlds.common.network.PayloadHandlerRegistry
import com.seleneworlds.common.network.packet.CustomPayloadPacket
import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.common.serialization.SerializedMapSerializer

/**
 * Send and handle custom payloads.
 */
class NetworkApi(
    private val networkClient: NetworkClient,
    private val json: Json,
    private val payloadRegistry: PayloadHandlerRegistry<Unit>
) {
    private val connectionLock = Any()
    private val connectionCallbacks = mutableSetOf<() -> Unit>()
    private var connectionReady = false

    fun handlePayload(payloadId: String, callback: (SerializedMap) -> Unit): () -> Unit {
        return payloadRegistry.registerHandler(payloadId) {
            _, payload -> callback(payload)
        }
    }

    fun sendToServer(payloadId: String, payload: SerializedMap) {
        networkClient.send(CustomPayloadPacket(payloadId, json.encodeToString(SerializedMapSerializer, payload)))
    }

    fun onConnected(callback: () -> Unit): () -> Unit {
        val runNow = synchronized(connectionLock) {
            if (connectionReady) true else {
                connectionCallbacks.add(callback)
                false
            }
        }
        if (runNow) callback()
        return { synchronized(connectionLock) { connectionCallbacks.remove(callback) } }
    }

    fun markConnectionReady() {
        val callbacks = synchronized(connectionLock) {
            connectionReady = true
            connectionCallbacks.toList().also { connectionCallbacks.clear() }
        }
        callbacks.forEach { it() }
    }
}

package com.seleneworlds.client.ui.cef

import com.seleneworlds.client.network.NetworkApi
import com.seleneworlds.common.serialization.SerializedMapSerializer
import com.seleneworlds.common.threading.MainThreadDispatcher
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.slf4j.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CefUiBridge(
    private val networkApi: NetworkApi,
    private val json: Json,
    private val mainThreadDispatcher: MainThreadDispatcher,
    private val logger: Logger,
    private val interactionState: CefInteractionState
) {
    private data class Subscription(val remove: () -> Unit, val callback: CefQueryCallback)

    private val subscriptions = ConcurrentHashMap<Long, Subscription>()
    private val receivedInteractionState = AtomicBoolean()
    private val uiReady = CountDownLatch(1)
    private val handler = object : CefMessageRouterHandlerAdapter() {
        override fun onQuery(browser: CefBrowser, frame: CefFrame, queryId: Long, request: String,
            persistent: Boolean, callback: CefQueryCallback): Boolean {
            return try {
                require(frame.isMain && CefTrustedDocuments.contains(frame.url)) {
                    "CEF UI bridge requests are restricted to trusted main-frame documents"
                }
                val message = json.parseToJsonElement(request).jsonObject
                when (message.requiredString("type")) {
                    "send" -> handleSend(message, callback)
                    "subscribe" -> handleSubscribe(browser, frame, queryId, persistent, message, callback)
                    "interactiveElements" -> handleInteractiveElements(message, callback)
                    else -> return false
                }
                true
            } catch (error: Exception) {
                logger.warn("Rejected CEF UI bridge request", error)
                callback.failure(1, error.message ?: "Invalid bridge request")
                true
            }
        }

        override fun onQueryCanceled(browser: CefBrowser, frame: CefFrame, queryId: Long) {
            subscriptions.remove(queryId)?.remove?.invoke()
        }
    }

    private var router: CefMessageRouter? = null

    fun initialize(): CefMessageRouter {
        check(router == null) { "CEF UI bridge is already initialized" }
        return CefMessageRouter.create(
            CefMessageRouter.CefMessageRouterConfig("seleneQuery", "seleneQueryCancel"),
            handler
        ).also { router = it }
    }

    private fun handleSend(message: JsonObject, callback: CefQueryCallback) {
        val payloadId = validatedPayloadId(message.requiredString("payloadId"))
        val payloadElement = message["payload"] ?: JsonObject(emptyMap())
        val payloadBytes = payloadElement.toString().toByteArray().size
        require(payloadBytes <= MAX_PAYLOAD_BYTES) { "Payload exceeds $MAX_PAYLOAD_BYTES bytes" }
        val payload = json.decodeFromJsonElement(SerializedMapSerializer, payloadElement)
        mainThreadDispatcher.runOnMainThread {
            networkApi.sendToServer(payloadId, payload)
            callback.success("")
        }
    }

    private fun handleSubscribe(browser: CefBrowser, frame: CefFrame, queryId: Long, persistent: Boolean,
        message: JsonObject, callback: CefQueryCallback) {
        require(persistent) { "Payload subscriptions must be persistent queries" }
        require(subscriptions.size < MAX_SUBSCRIPTIONS) { "UI subscription limit reached" }
        val payloadId = validatedPayloadId(message.requiredString("payloadId"))
        val subscriptionId = message.requiredString("subscriptionId")
        require(subscriptionId.length <= 32) { "Invalid UI subscription ID" }
        val remove = networkApi.handlePayload(payloadId) { payload ->
            try {
                val encodedId = JsonPrimitive(subscriptionId).toString()
                val encodedPayload = JsonPrimitive(
                    json.encodeToJsonElement(SerializedMapSerializer, payload).toString()).toString()
                browser.executeJavaScript(
                    "window.__seleneBridge?.payload($encodedId,$encodedPayload)", frame.url, 0)
            } catch (error: Exception) {
                logger.warn("Failed to deliver payload {} to CEF UI", payloadId, error)
            }
        }
        subscriptions.put(queryId, Subscription(remove, callback))?.remove?.invoke()
    }

    private fun handleInteractiveElements(message: JsonObject, callback: CefQueryCallback) {
        val regions = message["regions"]?.jsonArray.orEmpty().map { element ->
            val region = element.jsonObject
            CefHitRegion(
                x = region.requiredInt("x"),
                y = region.requiredInt("y"),
                width = region.requiredInt("width"),
                height = region.requiredInt("height")
            )
        }.filter { it.width > 0 && it.height > 0 }
        val editableFocused = message["editableFocused"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        val capturedKeys = message.stringSet("capturedKeys")
        val captureText = message["captureText"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
        val passthroughKeys = message.stringSet("passthroughKeys")
        interactionState.update(regions, editableFocused, capturedKeys, captureText, passthroughKeys)
        uiReady.countDown()
        if (receivedInteractionState.compareAndSet(false, true)) {
            logger.info("CEF UI reported {} interactive hit regions", regions.size)
        }
        callback.success("")
    }

    fun dispose() {
        uiReady.countDown()
        val currentRouter = router ?: return
        currentRouter.cancelPending(null, handler)
        subscriptions.values.forEach { it.remove() }
        subscriptions.clear()
        currentRouter.removeHandler(handler)
        currentRouter.dispose()
        router = null
    }

    fun awaitUiReady(timeout: Long, unit: TimeUnit): Boolean = uiReady.await(timeout, unit)

    fun prepareReload(browser: CefBrowser) {
        router?.cancelPending(browser, handler)
        interactionState.clear()
    }

    private fun validatedPayloadId(value: String): String {
        require(value.isNotEmpty() && value.length <= MAX_PAYLOAD_ID_LENGTH) {
            "Payload ID must contain 1-$MAX_PAYLOAD_ID_LENGTH characters"
        }
        return value
    }

    private fun JsonObject.requiredString(name: String): String =
        this[name]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing $name")

    private fun JsonObject.requiredInt(name: String): Int =
        this[name]?.jsonPrimitive?.content?.toIntOrNull() ?: throw IllegalArgumentException("Invalid $name")

    private fun JsonObject.stringSet(name: String): Set<String> = this[name]?.jsonArray.orEmpty().map { element ->
        element.jsonPrimitive.content.also { require(it.isNotEmpty() && it.length <= MAX_KEY_NAME_LENGTH) }
    }.also { require(it.size <= MAX_INPUT_KEYS) }.toSet()

    private companion object {
        const val MAX_PAYLOAD_ID_LENGTH = 128
        const val MAX_PAYLOAD_BYTES = 64 * 1024
        const val MAX_SUBSCRIPTIONS = 32767
        const val MAX_KEY_NAME_LENGTH = 64
        const val MAX_INPUT_KEYS = 256
    }
}

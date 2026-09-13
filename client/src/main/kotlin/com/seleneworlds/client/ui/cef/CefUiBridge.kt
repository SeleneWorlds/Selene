package com.seleneworlds.client.ui.cef

import com.seleneworlds.client.network.NetworkApi
import com.seleneworlds.client.camera.CameraManager
import com.seleneworlds.client.game.ClientEvents
import com.seleneworlds.client.maps.ClientMap
import com.seleneworlds.client.rendering.visual.VisualDefinition
import com.seleneworlds.client.rendering.visual.VisualRegistry
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.entities.VisualComponentConfiguration
import com.seleneworlds.common.serialization.SerializedMapSerializer
import com.seleneworlds.common.threading.MainThreadDispatcher
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
    private val interactionState: CefInteractionState,
    private val cameraManager: CameraManager,
    private val clientMap: ClientMap,
    private val bundleUiStorage: BundleUiStorage,
    private val visualRegistry: VisualRegistry
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
                    "sendPayloadToServer" -> sendPayloadToServer(message, callback)
                    "subscribeToServerPayload" -> subscribeToServerPayload(
                        browser, frame, queryId, persistent, message, callback)
                    "updateInteractionState" -> updateInteractionState(message, callback)
                    "getInitialWorldState" -> getInitialWorldState(callback)
                    "subscribeToWorldUpdates" -> subscribeToWorldUpdates(
                        browser, frame, queryId, persistent, callback)
                    "loadBundleStorageValue" -> loadBundleStorageValue(message, callback)
                    "saveBundleStorageValue" -> saveBundleStorageValue(message, callback)
                    "getVisualDefinition" -> getVisualDefinition(message, callback)
                    "getEntitiesAt" -> getEntitiesAt(message, callback)
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

    fun createMessageRouter(): CefMessageRouter {
        check(router == null) { "CEF UI bridge is already initialized" }
        return CefMessageRouter.create(
            CefMessageRouter.CefMessageRouterConfig("seleneBridgeRequest", "cancelSeleneBridgeRequest"),
            handler
        ).also { router = it }
    }

    private fun sendPayloadToServer(message: JsonObject, callback: CefQueryCallback) {
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

    private fun subscribeToServerPayload(browser: CefBrowser, frame: CefFrame, queryId: Long, persistent: Boolean,
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
                    "window.__seleneBridge?.deliverServerPayload($encodedId,$encodedPayload)", frame.url, 0)
            } catch (error: Exception) {
                logger.warn("Failed to deliver payload {} to CEF UI", payloadId, error)
            }
        }
        subscriptions.put(queryId, Subscription(remove, callback))?.remove?.invoke()
    }

    private fun updateInteractionState(message: JsonObject, callback: CefQueryCallback) {
        val regionElements = message["regions"]?.jsonArray.orEmpty()
        require(regionElements.size <= MAX_HIT_REGIONS) { "Interactive region limit reached" }
        val regions = regionElements.map { element ->
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

    private fun getInitialWorldState(callback: CefQueryCallback) {
        // UI initialization waits for CEF readiness on the game thread, so this
        // startup query must not dispatch back to that thread or both will wait
        // for each other. The game state is stationary during this handshake.
        // TODO Should move the wait off-thread esp. to also be able to actually render a progress bar or something
        val center = cameraManager.focusCoordinate
        callback.success(buildJsonObject {
            put("camera", coordinateJson(center))
            put("tiles", buildJsonArray {
                    for (y in center.y - WORLD_SNAPSHOT_RADIUS until center.y + WORLD_SNAPSHOT_RADIUS) {
                        for (x in center.x - WORLD_SNAPSHOT_RADIUS until center.x + WORLD_SNAPSHOT_RADIUS) {
                            mapTileJson(Coordinate(x, y, center.z))?.let(::add)
                    }
                }
            })
        }.toString())
    }

    private fun loadBundleStorageValue(message: JsonObject, callback: CefQueryCallback) {
        val value = bundleUiStorage.load(
            message.requiredString("bundle"),
            message.requiredString("entrypoint"),
            message.requiredString("key")
        )
        callback.success(buildJsonObject { value?.let { put("value", it) } }.toString())
    }

    private fun saveBundleStorageValue(message: JsonObject, callback: CefQueryCallback) {
        bundleUiStorage.save(
            message.requiredString("bundle"),
            message.requiredString("entrypoint"),
            message.requiredString("key"),
            message.requiredString("value")
        )
        callback.success("")
    }

    private fun getVisualDefinition(message: JsonObject, callback: CefQueryCallback) {
        val value = message.requiredString("identifier")
        require(value.length <= MAX_VISUAL_IDENTIFIER_LENGTH) { "Visual identifier is too long" }
        val identifier = Identifier.parse(value)
        val definition = requireNotNull(visualRegistry.get(identifier)) { "Visual not found: $identifier" }
        callback.success(json.encodeToJsonElement(VisualDefinition.serializer(), definition).toString())
    }

    private fun getEntitiesAt(message: JsonObject, callback: CefQueryCallback) {
        val coordinate = Coordinate(message.requiredInt("x"), message.requiredInt("y"), message.requiredInt("z"))
        mainThreadDispatcher.runOnMainThread {
            callback.success(buildJsonArray {
                clientMap.getEntitiesAt(coordinate).forEach { entity ->
                    val definition = entity.entityDefinition.get() ?: return@forEach
                    val visual = (definition.components["illarion:visual"] as? VisualComponentConfiguration)?.visual
                    add(buildJsonObject {
                        put("networkId", entity.networkId)
                        put("tags", buildJsonArray { definition.tags.forEach { add(JsonPrimitive(it)) } })
                        visual?.let { put("visual", it.toString()) }
                    })
                }
            }.toString())
        }
    }

    private fun subscribeToWorldUpdates(browser: CefBrowser, frame: CefFrame, queryId: Long, persistent: Boolean,
        callback: CefQueryCallback) {
        require(persistent) { "World subscriptions must be persistent queries" }
        val cameraListener = ClientEvents.CameraCoordinateChanged { coordinate ->
            val encoded = JsonPrimitive(coordinateJson(coordinate).toString()).toString()
            browser.executeJavaScript("window.__seleneBridge?.updateWorldCameraCoordinate($encoded)", frame.url, 0)
        }
        val mapListener = ClientEvents.MapChunkChanged { coordinate, width, height ->
            val changes = buildJsonArray {
                for (dy in 0 until height) for (dx in 0 until width) {
                    val tileCoordinate = Coordinate(coordinate.x + dx, coordinate.y + dy, coordinate.z)
                    add(mapTileJson(tileCoordinate) ?: buildJsonObject {
                        put("x", tileCoordinate.x); put("y", tileCoordinate.y); put("z", tileCoordinate.z)
                        put("removed", true)
                    })
                }
            }
            val encoded = JsonPrimitive(changes.toString()).toString()
            browser.executeJavaScript("window.__seleneBridge?.updateWorldMapTiles($encoded)", frame.url, 0)
        }
        ClientEvents.CameraCoordinateChanged.EVENT.register(cameraListener)
        ClientEvents.MapChunkChanged.EVENT.register(mapListener)
        val remove = {
            ClientEvents.CameraCoordinateChanged.EVENT.unregister(cameraListener)
            ClientEvents.MapChunkChanged.EVENT.unregister(mapListener)
        }
        subscriptions.put(queryId, Subscription(remove, callback))?.remove?.invoke()
    }

    private fun mapTileJson(coordinate: Coordinate): JsonObject? {
        val tile = clientMap.getTilesAt(coordinate).firstOrNull() ?: return null
        val colorIndex = (tile.visual.api.getMetadata("mapColorIndex") as? Number)?.toInt()
        return buildJsonObject {
            put("x", coordinate.x); put("y", coordinate.y); put("z", coordinate.z)
            put("visualMetadata", buildJsonObject {
                colorIndex?.let { put("mapColorIndex", it) }
            })
        }
    }

    private fun coordinateJson(coordinate: Coordinate) = buildJsonObject {
        put("x", coordinate.x); put("y", coordinate.y); put("z", coordinate.z)
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

    fun prepareForReload(browser: CefBrowser) {
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
        const val MAX_HIT_REGIONS = 4096
        const val MAX_KEY_NAME_LENGTH = 64
        const val MAX_INPUT_KEYS = 256
        const val MAX_VISUAL_IDENTIFIER_LENGTH = 256
        const val WORLD_SNAPSHOT_RADIUS = 80
    }
}

package com.seleneworlds.client.ui.cef

import com.seleneworlds.client.config.ClientConfig
import com.seleneworlds.client.config.ClientRuntimeConfig
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.client.rendering.visual.VisualDefinition
import com.seleneworlds.client.rendering.visual.VisualRegistry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.callback.CefResourceReadCallback
import org.cef.callback.CefResourceSkipCallback
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandler
import org.cef.handler.CefResourceHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.misc.IntRef
import org.cef.misc.LongRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import java.net.URI
import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicReference

class CefBundleScheme(
    private val bundleDatabase: BundleDatabase,
    runtimeConfig: ClientRuntimeConfig,
    clientConfig: ClientConfig,
    private val json: Json,
    private val visualRegistry: VisualRegistry
) : CefSchemeHandlerFactory {
    private val runtimePage = AtomicReference<Path?>()
    private val insecureBrowserUi = clientConfig.browserUiInsecure && clientConfig.browserUiUrl.isNotBlank()
    val contentSecurityPolicy = if (insecureBrowserUi) null else contentSecurityPolicy(
        runtimeConfig.contentServerUrl, clientConfig.browserUiUrl
    )

    fun setRuntimePage(path: Path?) {
        runtimePage.set(path?.toAbsolutePath()?.normalize())
    }

    fun bundleUrl(bundle: String, path: String): String =
        URI(SCHEME, HOST, "/bundle/$bundle/${path.replace('\\', '/')}", null).toASCIIString()

    val runtimeUrl: String get() = RUNTIME_URL

    override fun create(
        browser: CefBrowser, frame: CefFrame, schemeName: String,
        request: CefRequest
    ): CefResourceHandler {
        val resolved = resolve(request.url)
        return Resource(resolved?.path, resolved?.content, resolved?.mimeType, request.method, contentSecurityPolicy)
    }

    private fun resolve(url: String): ResolvedResource? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (uri.scheme != SCHEME || uri.host != HOST) return null
        val segments = uri.path.removePrefix("/").split('/').filter(String::isNotEmpty)
        if (segments == listOf("runtime", "index.html")) {
            return runtimePage.get()?.takeIf(Files::isRegularFile)?.let { ResolvedResource(path = it) }
        }
        if (segments == listOf("client", "asset-manifest.json")) return clientAssetManifest()
        if (segments == listOf("client", "registries", "selene:visuals")) return visualRegistrySnapshot()
        if (segments.size >= 3 && segments.take(2) == listOf("client", "content")) {
            return resolveAssetPath(segments.drop(2).joinToString("/"))?.let { ResolvedResource(path = it) }
        }
        if (segments.size < 3 || segments.first() != "bundle") return null
        val bundle = bundleDatabase.getEnabledBundle(segments[1]) ?: return null
        val root = runCatching { bundle.dir.toPath().toRealPath() }.getOrNull() ?: return null
        val candidate = root.resolve(segments.drop(2).joinToString("/")).normalize()
        if (!candidate.startsWith(root)) return null
        val resource = runCatching { candidate.toRealPath() }.getOrNull() ?: return null
        return resource.takeIf { it.startsWith(root) && Files.isRegularFile(it) }?.let { ResolvedResource(path = it) }
    }

    private fun resolveAssetPath(path: String): Path? {
        if (!path.startsWith("client/") && !path.startsWith("common/")) return null
        return bundleDatabase.enabledBundles.asReversed().firstNotNullOfOrNull { bundle ->
            val root = runCatching { bundle.dir.toPath().toRealPath() }.getOrNull() ?: return@firstNotNullOfOrNull null
            val candidate = root.resolve(path).normalize()
            if (!candidate.startsWith(root)) return@firstNotNullOfOrNull null
            runCatching { candidate.toRealPath() }.getOrNull()?.takeIf { it.startsWith(root) && Files.isRegularFile(it) }
        }
    }

    private fun clientAssetManifest(): ResolvedResource {
        val assets = linkedMapOf<String, JsonPrimitive>()
        for (bundle in bundleDatabase.enabledBundles) for (rootName in listOf("common", "client")) {
            val root = bundle.dir.toPath().resolve(rootName)
            if (!Files.isDirectory(root)) continue
            Files.walk(root).use { paths -> paths.filter(Files::isRegularFile).forEach { file ->
                val logicalPath = "$rootName/${root.relativize(file).toString().replace('\\', '/')}"
                assets[logicalPath] = JsonPrimitive("/client/content/$logicalPath")
            } }
        }
        val content = buildJsonObject { put("assets", JsonObject(assets)) }.toString().toByteArray()
        return ResolvedResource(content = content, mimeType = "application/json")
    }

    private fun visualRegistrySnapshot(): ResolvedResource {
        val entries = visualRegistry.getAll().mapKeys { it.key.toString() }.mapValues {
            json.encodeToJsonElement(VisualDefinition.serializer(), it.value)
        }
        val content = buildJsonObject {
            put("registry", "selene:visuals")
            put("hash", visualRegistry.cacheKey.toString())
            put("entries", JsonObject(entries))
        }.toString().toByteArray()
        return ResolvedResource(content = content, mimeType = "application/json")
    }

    private data class ResolvedResource(
        val path: Path? = null,
        val content: ByteArray? = null,
        val mimeType: String? = null
    )

    private class Resource(
        private val path: Path?,
        private val content: ByteArray?,
        explicitMimeType: String?,
        private val method: String,
        private val contentSecurityPolicy: String?
    ) : CefResourceHandlerAdapter() {
        private val mimeType = explicitMimeType ?: path?.let(::mimeType) ?: "text/plain"
        private var channel: SeekableByteChannel? = null
        private var contentOffset = 0
        private var openFailed = false

        override fun open(request: CefRequest, handleRequest: BoolRef, callback: CefCallback): Boolean {
            if (path != null && method == "GET") {
                channel = runCatching { Files.newByteChannel(path, StandardOpenOption.READ) }
                    .getOrElse {
                        openFailed = true
                        null
                    }
            }
            handleRequest.set(true)
            return true
        }

        override fun getResponseHeaders(
            response: CefResponse, responseLength: IntRef,
            redirectUrl: StringRef
        ) {
            response.mimeType = mimeType
            contentSecurityPolicy?.let {
                response.setHeaderByName("Content-Security-Policy", it, true)
            }
            response.setHeaderByName("X-Content-Type-Options", "nosniff", true)
            when {
                method != "GET" && method != "HEAD" -> {
                    response.status = 405
                    response.statusText = "Method Not Allowed"
                    responseLength.set(0)
                }

                path == null && content == null || openFailed -> {
                    response.status = 404
                    response.statusText = "Not Found"
                    responseLength.set(0)
                }

                else -> {
                    val length = content?.size?.toLong() ?: path?.let { runCatching { Files.size(it) }.getOrNull() }
                    if (length == null) {
                        closeResource()
                        response.status = 404
                        response.statusText = "Not Found"
                        responseLength.set(0)
                    } else {
                        response.status = 200
                        response.statusText = "OK"
                        responseLength.set(if (method == "HEAD") 0 else length.toInt())
                    }
                }
            }
        }

        override fun read(
            dataOut: ByteArray, bytesToRead: Int, bytesRead: IntRef,
            callback: CefResourceReadCallback
        ): Boolean {
            if (content != null) {
                val count = minOf(bytesToRead, content.size - contentOffset)
                if (count <= 0) return false
                content.copyInto(dataOut, 0, contentOffset, contentOffset + count)
                contentOffset += count
                bytesRead.set(count)
                return true
            }
            val resource = channel ?: return false
            val count = runCatching { resource.read(ByteBuffer.wrap(dataOut, 0, bytesToRead)) }
                .getOrElse {
                    closeResource()
                    return false
                }
            if (count <= 0) {
                closeResource()
                return false
            }
            bytesRead.set(count)
            if (resource.position() >= resource.size()) closeResource()
            return true
        }

        override fun skip(
            bytesToSkip: Long, bytesSkipped: LongRef,
            callback: CefResourceSkipCallback
        ): Boolean {
            if (content != null) {
                val count = minOf(bytesToSkip, (content.size - contentOffset).toLong()).coerceAtLeast(0).toInt()
                contentOffset += count
                bytesSkipped.set(count.toLong())
                return count > 0
            }
            val resource = channel ?: return false
            val count = runCatching {
                val position = resource.position()
                val target = minOf(position + bytesToSkip, resource.size())
                resource.position(target)
                target - position
            }.getOrElse {
                closeResource()
                return false
            }
            bytesSkipped.set(count)
            if (resource.position() >= resource.size()) closeResource()
            return count > 0
        }

        override fun cancel() = closeResource()

        private fun closeResource() {
            runCatching { channel?.close() }
            channel = null
        }

        private companion object {
            private val MIME_TYPES = mapOf(
                "html" to "text/html",
                "htm" to "text/html",
                "js" to "text/javascript",
                "mjs" to "text/javascript",
                "css" to "text/css",
                "json" to "application/json",
                "svg" to "image/svg+xml"
            )

            fun mimeType(path: Path): String {
                val extension = path.fileName.toString().substringAfterLast('.', "").lowercase()
                return MIME_TYPES[extension]
                    ?: runCatching { Files.probeContentType(path) }.getOrNull()
                    ?: "application/octet-stream"
            }

        }
    }

    companion object {
        const val SCHEME = "https"
        const val HOST = "cef.seleneworlds.com"
        const val RUNTIME_URL = "$SCHEME://$HOST/runtime/index.html"
        const val CSP_NONCE = "selene-runtime"

        private fun contentSecurityPolicy(vararg configuredUrls: String): String {
            val remoteOrigins = configuredUrls.mapNotNull { configuredUrl -> runCatching {
                val uri = URI(configuredUrl.trim())
                require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true))
                require(uri.host != null && uri.userInfo == null)
                URI(uri.scheme.lowercase(), null, uri.host, uri.port, null, null, null).toASCIIString()
            }.getOrNull() }.distinct()
            val sources = (listOf("'self'") + remoteOrigins).joinToString(" ")
            val connectionSources = (listOf("'self'") + remoteOrigins + remoteOrigins.mapNotNull { origin ->
                val uri = URI(origin)
                val scheme = when (uri.scheme) {
                    "http" -> "ws"
                    "https" -> "wss"
                    else -> return@mapNotNull null
                }
                URI(scheme, null, uri.host, uri.port, null, null, null).toASCIIString()
            }).distinct().joinToString(" ")
            return "default-src $sources; script-src $sources 'nonce-$CSP_NONCE'; " +
                "connect-src $connectionSources; style-src $sources 'unsafe-inline'; " +
                "img-src $sources data:; frame-src 'none'"
        }
    }
}

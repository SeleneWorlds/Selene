package com.seleneworlds.client.ui.cef

import com.seleneworlds.common.bundles.BundleDatabase
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.callback.CefResourceReadCallback
import org.cef.callback.CefResourceSkipCallback
import org.cef.callback.CefSchemeHandlerFactory
import org.cef.handler.CefResourceHandlerAdapter
import org.cef.handler.CefResourceHandler
import org.cef.misc.BoolRef
import org.cef.misc.IntRef
import org.cef.misc.LongRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

class CefBundleScheme(private val bundleDatabase: BundleDatabase) : CefSchemeHandlerFactory {
    private val runtimePage = AtomicReference<Path?>()

    fun setRuntimePage(path: Path?) {
        runtimePage.set(path?.toAbsolutePath()?.normalize())
    }

    fun bundleUrl(bundle: String, path: String): String =
        URI(SCHEME, HOST, "/bundle/$bundle/${path.replace('\\', '/')}", null).toASCIIString()

    val runtimeUrl: String get() = RUNTIME_URL

    override fun create(browser: CefBrowser, frame: CefFrame, schemeName: String,
        request: CefRequest): CefResourceHandler = Resource(resolve(request.url), request.method)

    private fun resolve(url: String): Path? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        if (uri.scheme != SCHEME || uri.host != HOST) return null
        val segments = uri.path.removePrefix("/").split('/').filter(String::isNotEmpty)
        if (segments == listOf("runtime", "index.html")) return runtimePage.get()?.takeIf(Files::isRegularFile)
        if (segments.size < 3 || segments.first() != "bundle") return null
        val bundle = bundleDatabase.getEnabledBundle(segments[1]) ?: return null
        val root = runCatching { bundle.dir.toPath().toRealPath() }.getOrNull() ?: return null
        val candidate = root.resolve(segments.drop(2).joinToString("/")).normalize()
        if (!candidate.startsWith(root)) return null
        val resource = runCatching { candidate.toRealPath() }.getOrNull() ?: return null
        return resource.takeIf { it.startsWith(root) && Files.isRegularFile(it) }
    }

    private class Resource(path: Path?, private val method: String) : CefResourceHandlerAdapter() {
        private val content = path?.let { runCatching { Files.readAllBytes(it) }.getOrNull() }
        private val mimeType = path?.let(::mimeType) ?: "text/plain"
        private var offset = 0

        override fun open(request: CefRequest, handleRequest: BoolRef, callback: CefCallback): Boolean {
            handleRequest.set(true)
            return true
        }

        override fun getResponseHeaders(response: CefResponse, responseLength: IntRef,
            redirectUrl: StringRef) {
            response.mimeType = mimeType
            when {
                method != "GET" && method != "HEAD" -> {
                    response.status = 405
                    response.statusText = "Method Not Allowed"
                    responseLength.set(0)
                }
                content == null -> {
                    response.status = 404
                    response.statusText = "Not Found"
                    responseLength.set(0)
                }
                else -> {
                    response.status = 200
                    response.statusText = "OK"
                    responseLength.set(if (method == "HEAD") 0 else content.size)
                }
            }
        }

        override fun read(dataOut: ByteArray, bytesToRead: Int, bytesRead: IntRef,
            callback: CefResourceReadCallback): Boolean {
            val bytes = content ?: return false
            val count = minOf(bytesToRead, bytes.size - offset)
            if (count <= 0 || method == "HEAD") return false
            bytes.copyInto(dataOut, destinationOffset = 0, startIndex = offset, endIndex = offset + count)
            offset += count
            bytesRead.set(count)
            return true
        }

        override fun skip(bytesToSkip: Long, bytesSkipped: LongRef,
            callback: CefResourceSkipCallback): Boolean {
            val bytes = content ?: return false
            val count = minOf(bytesToSkip, (bytes.size - offset).toLong())
            offset += count.toInt()
            bytesSkipped.set(count)
            return count > 0
        }

        private companion object {
            fun mimeType(path: Path): String = Files.probeContentType(path) ?: when {
                path.toString().endsWith(".html") -> "text/html"
                path.toString().endsWith(".js") -> "text/javascript"
                path.toString().endsWith(".css") -> "text/css"
                path.toString().endsWith(".json") -> "application/json"
                path.toString().endsWith(".svg") -> "image/svg+xml"
                else -> "application/octet-stream"
            }
        }
    }

    companion object {
        const val SCHEME = "https"
        const val HOST = "cef.seleneworlds.com"
        const val RUNTIME_URL = "$SCHEME://$HOST/runtime/index.html"
    }
}

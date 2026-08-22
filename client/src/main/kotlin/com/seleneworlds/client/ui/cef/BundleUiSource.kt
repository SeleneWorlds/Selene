package com.seleneworlds.client.ui.cef

import com.seleneworlds.client.config.ClientRuntimeConfig
import com.seleneworlds.common.bundles.BundleDatabase
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import java.net.URI

class BundleUiSource(
    private val runtimeConfig: ClientRuntimeConfig,
    private val bundleDatabase: BundleDatabase,
    private val httpClient: HttpClient,
    private val json: Json,
    private val logger: Logger,
    private val bundleScheme: CefBundleScheme
) {
    fun load(): List<BrowserUiEntrypoint> {
        if (runtimeConfig.contentServerUrl.isNotBlank()) {
            try {
                return loadRemote()
            } catch (error: Exception) {
                logger.warn("Failed to load remote client UI index; falling back to local bundles", error)
            }
        }
        return loadLocal()
    }

    private fun loadRemote(): List<BrowserUiEntrypoint> = runBlocking {
        val baseUrl = runtimeConfig.contentServerUrl.trimEnd('/')
        val index = httpClient.get("$baseUrl/client/ui") {
            if (runtimeConfig.token.isNotBlank()) header(HttpHeaders.Authorization, "Bearer ${runtimeConfig.token}")
        }.body<ClientUiIndex>()
        index.entrypoints.map { entry ->
            val resolvedUrl = URI("$baseUrl/").resolve(entry.url).toASCIIString()
            BrowserUiEntrypoint(entry.bundle, entry.id, resolvedUrl, null)
        }
    }

    private fun loadLocal(): List<BrowserUiEntrypoint> = bundleDatabase.enabledBundles.flatMap { bundle ->
        bundle.manifest.ui.mapNotNull { specification ->
            val file = bundle.dir.resolve(specification.entrypoint).absoluteFile
            if (!file.isFile) {
                logger.warn("Ignoring missing UI entrypoint {}:{}", bundle.manifest.name, specification.id)
                null
            } else {
                BrowserUiEntrypoint(bundle.manifest.name, specification.id,
                    bundleScheme.bundleUrl(bundle.manifest.name, specification.entrypoint), file.readText())
            }
        }
    }

    fun encode(entries: List<BrowserUiEntrypoint>): String =
        json.encodeToString(entries).replace("<", "\\u003c")
}

@Serializable
data class BrowserUiEntrypoint(val bundle: String, val id: String, val url: String, val html: String?)

@Serializable
private data class ClientUiIndex(val entrypoints: List<ClientUiIndexEntry>)

@Serializable
private data class ClientUiIndexEntry(val bundle: String, val id: String, val url: String)

package com.seleneworlds.client.assets

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import org.slf4j.Logger
import com.seleneworlds.client.config.ClientRuntimeConfig
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.network.packet.NotifyBundleUpdatePacket
import com.seleneworlds.common.serialization.seleneJson
import com.seleneworlds.common.util.Disposable
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class RuntimeBundleUpdateManager(
    private val logger: Logger,
    private val httpClient: HttpClient,
    private val bundleDatabase: BundleDatabase,
    private val runtimeConfig: ClientRuntimeConfig
) : Disposable {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val manifestByBundle = ConcurrentHashMap<String, RuntimeAssetManifest>()

    private val contentServerUrl: String get() = runtimeConfig.contentServerUrl

    fun handleBundleContentUpdate(packet: NotifyBundleUpdatePacket) {
        scope.launch {
            try {
                logger.info("Processing bundle content update for bundle: ${packet.bundleId}")
                logger.debug("Updated files: {}", packet.updated)
                logger.debug("Deleted files: {}", packet.deleted)

                val bundle = bundleDatabase.getBundle(packet.bundleId)
                if (bundle == null) {
                    logger.warn("Received bundle content update for unknown bundle: ${packet.bundleId}")
                    return@launch
                }
                manifestByBundle.remove(bundle.manifest.name)

                // Download updated files and notify registry
                for (filePath in packet.updated) {
                    downloadBundleContentFile(bundle, filePath)
                }

                // Remove deleted files and notify registry
                for (filePath in packet.deleted) {
                    deleteBundleContentFile(bundle, filePath)
                }
            } catch (e: Exception) {
                logger.error("Failed to process content update for bundle: ${packet.bundleId}", e)
            }
        }
    }

    private suspend fun downloadBundleContentFile(bundle: Bundle, filePath: String) {
        if (contentServerUrl.isEmpty()) {
            logger.warn("Content server URL is empty, skipping bundle content update")
            return
        }

        try {
            val manifest = getBundleAssetManifest(bundle)
            val hashedUrl = manifest.assets[filePath]
            if (hashedUrl == null) {
                logger.warn("No hashed content URL found for updated bundle file: {} in {}", filePath, bundle.manifest.name)
                return
            }

            val url = "${contentServerUrl}${hashedUrl}"
            logger.debug("Downloading bundle content: {}", url)

            val response: HttpResponse = httpClient.get(url) {
                headers {
                    if (runtimeConfig.token.isNotBlank()) {
                        append("Authorization", "Bearer ${runtimeConfig.token}")
                    }
                }
            }
            if (response.status.value in 200..299) {
                val outputFile = bundle.dir.resolve(filePath)
                if (!isPathWithinBundle(bundle, outputFile.toPath())) {
                    logger.error("Forbidden: Attempted to update file outside bundle directory: $filePath")
                    throw SecurityException("File path $filePath is outside bundle directory")
                }

                outputFile.parentFile.mkdirs()

                val channel = response.bodyAsChannel()
                outputFile.outputStream().use { output ->
                    channel.copyTo(output)
                }

                logger.debug("Successfully downloaded content update: $filePath")
            } else {
                logger.warn("Failed to download content update $filePath: HTTP ${response.status.value}")
            }

        } catch (e: Exception) {
            logger.error("Failed to download content update: $filePath", e)
            throw e
        }
    }

    private suspend fun getBundleAssetManifest(bundle: Bundle): RuntimeAssetManifest {
        manifestByBundle[bundle.manifest.name]?.let { return it }

        val url = "${contentServerUrl}/bundles/${bundle.manifest.name}/asset-manifest.json"
        val response: HttpResponse = httpClient.get(url) {
            headers {
                if (runtimeConfig.token.isNotBlank()) {
                    append("Authorization", "Bearer ${runtimeConfig.token}")
                }
            }
        }
        if (response.status.value !in 200..299) {
            throw IllegalStateException(
                "Failed to download bundle asset manifest for ${bundle.manifest.name}: HTTP ${response.status.value}"
            )
        }

        val manifest = seleneJson.decodeFromString<RuntimeAssetManifest>(response.bodyAsText())
        manifestByBundle[bundle.manifest.name] = manifest
        return manifest
    }

    private fun deleteBundleContentFile(bundle: Bundle, filePath: String) {
        try {
            val targetFile = bundle.dir.resolve(filePath)
            if (!isPathWithinBundle(bundle, targetFile.toPath())) {
                logger.error("Forbidden: Attempted to delete outside bundle directory: $filePath")
                throw SecurityException("File path $filePath is outside bundle directory")
            }
            
            if (targetFile.exists()) {
                Files.delete(targetFile.toPath())
                logger.debug("Deleted bundle content file: $filePath")
            }
        } catch (e: Exception) {
            logger.error("Failed to delete bundle content file: $filePath", e)
            throw e
        }
    }

    private fun isPathWithinBundle(bundle: Bundle, path: Path): Boolean {
        return path.normalize().startsWith(bundle.dir.toPath().normalize())
    }

    override fun dispose() {
        scope.cancel()
    }
}

@Serializable
private data class RuntimeAssetManifest(
    val assets: Map<String, String>
)

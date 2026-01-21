package world.selene.client.assets

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import org.slf4j.Logger
import world.selene.client.config.ClientRuntimeConfig
import world.selene.client.data.Registries
import world.selene.common.bundles.Bundle
import world.selene.common.bundles.BundleDatabase
import world.selene.common.data.BundleDrivenRegistry
import world.selene.common.data.Identifier
import world.selene.common.network.packet.NotifyBundleUpdatePacket
import world.selene.common.util.Disposable
import java.nio.file.Files
import java.nio.file.Path

class RuntimeBundleUpdateManager(
    private val logger: Logger,
    private val httpClient: HttpClient,
    private val bundleDatabase: BundleDatabase,
    private val registries: Registries,
    private val runtimeConfig: ClientRuntimeConfig
) : Disposable {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

                // Download updated files and notify registry
                for (filePath in packet.updated) {
                    downloadBundleContentFile(bundle.manifest.name, filePath)
                    findRegistryForFile(filePath)?.bundleFileUpdated(bundleDatabase, bundle, filePath)
                }

                // Remove deleted files and notify registry
                for (filePath in packet.deleted) {
                    deleteBundleContentFile(bundle, filePath)
                    findRegistryForFile(filePath)?.bundleFileRemoved(bundleDatabase, bundle, filePath)
                }
            } catch (e: Exception) {
                logger.error("Failed to process content update for bundle: ${packet.bundleId}", e)
            }
        }
    }

    val registryFilePattern = "^(common|client)/data/[\\w-]+/([\\w-]+)/.*".toRegex()

    private fun findRegistryForFile(filePath: String): BundleDrivenRegistry? {
        return (registryFilePattern.find(filePath)?.groups[2]?.value?.let {
            val builtinRegistry = registries.getRegistry(Identifier.withDefaultNamespace(it))
            if (builtinRegistry != null) {
                return@let builtinRegistry
            }
            return@let registries.customRegistries.findByRegistryName(it)
        } as? BundleDrivenRegistry)
    }

    private suspend fun downloadBundleContentFile(bundleName: String, filePath: String) {
        try {
            val url = "${contentServerUrl}/bundles/$bundleName/content/$filePath"
            logger.debug("Downloading bundle content: {}", url)

            val response: HttpResponse = httpClient.get(url) {
                headers {
                    if (runtimeConfig.token.isNotBlank()) {
                        append("Authorization", "Bearer ${runtimeConfig.token}")
                    }
                }
            }
            if (response.status.value in 200..299) {
                val bundle = bundleDatabase.getBundle(bundleName)
                if (bundle != null) {
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
                    logger.warn("Bundle not found for content update download: $bundleName")
                }
            } else {
                logger.warn("Failed to download content update $filePath: HTTP ${response.status.value}")
            }

        } catch (e: Exception) {
            logger.error("Failed to download content update: $filePath", e)
            throw e
        }
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
        }
    }

    private fun isPathWithinBundle(bundle: Bundle, path: Path): Boolean {
        return path.normalize().startsWith(bundle.dir.toPath().normalize())
    }

    override fun dispose() {
        scope.cancel()
    }
}

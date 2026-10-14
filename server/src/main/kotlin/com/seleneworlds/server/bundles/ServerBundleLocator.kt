package com.seleneworlds.server.bundles

import kotlinx.serialization.json.Json
import org.slf4j.Logger
import com.seleneworlds.common.bundles.BundleLocator
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.bundleContentDirectories
import com.seleneworlds.common.serialization.decodeFromFile
import com.seleneworlds.server.config.ServerConfig
import java.io.File

class ServerBundleLocator(
    private val json: Json,
    private val config: ServerConfig,
    private val logger: Logger
) : BundleLocator {
    // Build once per locator so dependency resolution does not repeatedly scan the tree.
    private val bundlesByName: Map<String, Bundle> by lazy {
        val bundles = linkedMapOf<String, Bundle>()
        val root = File(config.bundlesPath)
        root.walkTopDown()
            // Allow direct bundles and one nested directory layer, including their manifests.
            .maxDepth(3)
            .onEnter { directory ->
                directory == root || (!directory.name.startsWith(".") &&
                    !(directory.name in bundleContentDirectories && directory.parentFile.resolve("bundle.json").isFile))
            }
            .filter { it.isFile && it.name == "bundle.json" }
            .sortedBy { it.invariantSeparatorsPath }
            .forEach { manifestFile ->
                val manifest = try {
                    json.decodeFromFile(BundleManifest.serializer(), manifestFile)
                } catch (e: Exception) {
                    logger.error("Failed to read bundle manifest at $manifestFile", e)
                    return@forEach
                }
                require(manifest.name.isNotBlank() && manifest.name !in setOf(".", "..") &&
                    '/' !in manifest.name && '\\' !in manifest.name) {
                    "Bundle names must be flat: '${manifest.name}' at $manifestFile"
                }
                val bundle = Bundle(manifest, manifestFile.parentFile)
                val previous = bundles.putIfAbsent(manifest.name, bundle)
                require(previous == null) {
                    "Duplicate bundle name '${manifest.name}' at ${previous?.dir} and ${bundle.dir}"
                }
            }
        bundles
    }

    override fun locateBundle(name: String): Bundle? = bundlesByName[name]
}

package world.selene.client.bundles

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import world.selene.client.config.ClientRuntimeConfig
import world.selene.common.bundles.BundleLocator
import world.selene.common.bundles.BundleManifest
import world.selene.common.bundles.Bundle
import java.io.File

class ClientBundleLocator(
    private val objectMapper: ObjectMapper,
    private val config: ClientRuntimeConfig,
    private val logger: Logger
) :
    BundleLocator {
    override fun locateBundle(name: String): Bundle? {
        val bundlePath = config.bundles[name] ?: return null
        val dir = File(bundlePath)
        val manifestFile = File(dir, "bundle.json")
        if (!manifestFile.exists()) return null
        val manifest = try {
            objectMapper.readValue(manifestFile, BundleManifest::class.java)
        } catch (e: Exception) {
            logger.error("Failed to read bundle manifest", e)
            return null
        }
        return Bundle(manifest, dir)
    }
}
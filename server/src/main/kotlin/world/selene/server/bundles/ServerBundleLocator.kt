package world.selene.server.bundles

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import world.selene.common.bundles.BundleLocator
import world.selene.common.bundles.BundleManifest
import world.selene.common.bundles.Bundle
import world.selene.server.config.ServerConfig
import java.io.File

class ServerBundleLocator(
    private val objectMapper: ObjectMapper,
    private val config: ServerConfig,
    private val logger: Logger
) : BundleLocator {
    override fun locateBundle(name: String): Bundle? {
        val dir = File(config.bundlesPath, name)
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
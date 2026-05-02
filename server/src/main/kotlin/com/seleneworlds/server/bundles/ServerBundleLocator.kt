package com.seleneworlds.server.bundles

import kotlinx.serialization.json.Json
import org.slf4j.Logger
import com.seleneworlds.common.bundles.BundleLocator
import com.seleneworlds.common.bundles.BundleManifest
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.serialization.decodeFromFile
import com.seleneworlds.server.config.ServerConfig
import java.io.File

class ServerBundleLocator(
    private val json: Json,
    private val config: ServerConfig,
    private val logger: Logger
) : BundleLocator {
    override fun locateBundle(name: String): Bundle? {
        val dir = File(config.bundlesPath, name)
        val manifestFile = File(dir, "bundle.json")
        if (!manifestFile.exists()) return null
        val manifest = try {
            json.decodeFromFile(BundleManifest.serializer(), manifestFile)
        } catch (e: Exception) {
            logger.error("Failed to read bundle manifest", e)
            return null
        }
        return Bundle(manifest, dir)
    }
}


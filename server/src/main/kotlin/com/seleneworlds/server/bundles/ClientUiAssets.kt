package com.seleneworlds.server.bundles

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.BundleUiEntrypoint
import kotlinx.serialization.Serializable
import java.io.File
import java.security.MessageDigest

class ClientUiAssets(
    private val bundleDatabase: BundleDatabase,
    private val clientBundleCache: ClientBundleCache
) {
    fun getIndex(): ClientUiIndexResponse {
        val entrypoints = enabledEntrypoints().map { (bundle, spec) ->
            ClientUiEntrypointResponse(
                bundle = bundle.manifest.name,
                id = spec.id,
                url = "/client/ui/content/${bundle.manifest.name}/${spec.id}/${spec.entrypoint.substringAfterLast('/')}"
            )
        }
        return ClientUiIndexResponse(
            hash = hashText(entrypoints.joinToString("|") {
                "${it.bundle}:${clientBundleCache.getHash(bundleDatabase.getEnabledBundle(it.bundle)!!.dir).orEmpty()}:${it.id}:${it.url}"
            }),
            entrypoints = entrypoints
        )
    }

    fun resolve(bundleName: String, id: String, requestedPath: String): File? {
        val (bundle, spec) = enabledEntrypoints().firstOrNull {
            it.first.manifest.name == bundleName && it.second.id == id
        } ?: return null
        val root = bundle.dir.resolve(spec.entrypoint).parentFile.canonicalFile
        val file = root.resolve(requestedPath).canonicalFile
        if (!file.toPath().startsWith(root.toPath()) || !file.isFile) return null
        return file
    }

    private fun enabledEntrypoints(): List<Pair<Bundle, BundleUiEntrypoint>> = bundleDatabase.enabledBundles
        .filter { clientBundleCache.hasClientSide(it.dir) }
        .flatMap { bundle -> bundle.manifest.ui.mapNotNull { spec ->
            val normalized = spec.entrypoint.replace('\\', '/')
            val valid = spec.id.matches(Regex("[A-Za-z0-9._-]+")) &&
                !normalized.startsWith("/") && normalized.split('/').none { it == ".." } &&
                (normalized.startsWith("client/") || normalized.startsWith("common/")) &&
                bundle.dir.resolve(normalized).isFile
            if (valid) bundle to spec else null
        } }

    private fun hashText(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}

@Serializable
data class ClientUiIndexResponse(val hash: String, val entrypoints: List<ClientUiEntrypointResponse>)

@Serializable
data class ClientUiEntrypointResponse(
    val bundle: String,
    val id: String,
    val url: String
)

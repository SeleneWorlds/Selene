package com.seleneworlds.server.bundles

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.getPreloadSpecs
import kotlinx.serialization.Serializable
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class ClientLuaModules(
    private val bundleDatabase: BundleDatabase,
    private val clientBundleCache: ClientBundleCache
) {
    private val snapshotsByCacheKey = ConcurrentHashMap<String, ClientLuaSnapshotData>()

    fun getIndex(): ClientLuaIndexResponse {
        val snapshot = getSnapshot()
        return ClientLuaIndexResponse(
            hash = snapshot.hash,
            entrypoints = snapshot.entrypoints,
            modules = snapshot.modules.mapValues { (_, module) ->
                ClientLuaModuleIndexEntry(
                    hash = module.hash,
                    url = "/client/lua/modules/${module.module}"
                )
            }.toSortedMap()
        )
    }

    fun getModule(moduleName: String): ClientLuaModuleSourceResponse? {
        return getSnapshot().modules[moduleName]
    }

    private fun getSnapshot(): ClientLuaSnapshotData {
        val enabledClientBundles = bundleDatabase.enabledBundles
            .filter { clientBundleCache.hasClientSide(it.dir) }

        val cacheKey = enabledClientBundles.joinToString("|") { bundle ->
            "${bundle.manifest.name}:${clientBundleCache.getHash(bundle.dir).orEmpty()}"
        }

        return snapshotsByCacheKey.getOrPut(cacheKey) {
            buildSnapshot(enabledClientBundles)
        }
    }

    private fun buildSnapshot(bundles: List<Bundle>): ClientLuaSnapshotData {
        val modules = mutableMapOf<String, ClientLuaModuleSourceResponse>()
        val entrypoints = mutableListOf<ClientLuaEntrypointIndexEntry>()

        for (bundle in bundles) {
            loadBundleModules(bundle, modules)

            for (entrypoint in bundle.manifest.entrypoints) {
                if (CLIENT_ENTRYPOINT_FILTERS.none { entrypoint.startsWith(it) }) {
                    continue
                }

                val moduleName = moduleNameForLuaFile(bundle, entrypoint) ?: continue
                if (modules[moduleName] == null) {
                    val sourceFile = bundle.dir.resolve(entrypoint)
                    if (sourceFile.isFile) {
                        modules[moduleName] = sourceResponse(bundle, moduleName, entrypoint, sourceFile.readText())
                    }
                }

                entrypoints.add(
                    ClientLuaEntrypointIndexEntry(
                        bundle = bundle.manifest.name,
                        path = entrypoint,
                        module = moduleName,
                        url = "/client/lua/modules/$moduleName"
                    )
                )
            }
        }

        val sortedModules = modules.toSortedMap()
        return ClientLuaSnapshotData(
            hash = hashText(sortedModules.values.joinToString("|") { "${it.module}:${it.hash}" }),
            entrypoints = entrypoints,
            modules = sortedModules
        )
    }

    private fun loadBundleModules(
        bundle: Bundle,
        modules: MutableMap<String, ClientLuaModuleSourceResponse>
    ) {
        bundle.dir.walkTopDown()
            .filter { it.isFile && it.extension == "lua" }
            .forEach { file ->
                val relativePath = file.relativeTo(bundle.dir).invariantSeparatorsPath
                if (!isClientLuaPath(relativePath)) {
                    return@forEach
                }

                val moduleName = moduleNameForLuaFile(bundle, relativePath) ?: return@forEach
                modules[moduleName] = sourceResponse(bundle, moduleName, relativePath, file.readText())
            }

        for (preloadSpec in bundle.manifest.getPreloadSpecs()) {
            val relativePath = preloadSpec.file.replace('\\', '/')
            if (!isClientLuaPath(relativePath)) {
                continue
            }

            val file = bundle.dir.resolve(relativePath)
            if (file.isFile) {
                modules[preloadSpec.moduleName] = sourceResponse(
                    bundle = bundle,
                    moduleName = preloadSpec.moduleName,
                    path = relativePath,
                    source = file.readText(preloadSpec.encoding)
                )
            }
        }
    }

    private fun sourceResponse(
        bundle: Bundle,
        moduleName: String,
        path: String,
        source: String
    ): ClientLuaModuleSourceResponse {
        return ClientLuaModuleSourceResponse(
            module = moduleName,
            bundle = bundle.manifest.name,
            path = path,
            hash = hashText(source),
            source = source
        )
    }

    private fun moduleNameForLuaFile(bundle: Bundle, relativePath: String): String? {
        if (!relativePath.endsWith(".lua")) {
            return null
        }

        if (relativePath == "init.lua") {
            return bundle.manifest.name
        }

        val modulePath = relativePath.removeSuffix(".lua").replace('/', '.')
        return "${bundle.manifest.name}.$modulePath"
    }

    private fun isClientLuaPath(relativePath: String): Boolean {
        return relativePath == "init.lua" ||
            relativePath.startsWith("common/") ||
            relativePath.startsWith("client/")
    }

    private fun hashText(text: String): String {
        val hasher = MessageDigest.getInstance("SHA-256")
        return hasher.digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private data class ClientLuaSnapshotData(
        val hash: String,
        val entrypoints: List<ClientLuaEntrypointIndexEntry>,
        val modules: Map<String, ClientLuaModuleSourceResponse>
    )

    private companion object {
        val CLIENT_ENTRYPOINT_FILTERS = listOf("common/", "client/", "init.lua")
    }
}

@Serializable
data class ClientLuaIndexResponse(
    val hash: String,
    val entrypoints: List<ClientLuaEntrypointIndexEntry>,
    val modules: Map<String, ClientLuaModuleIndexEntry>
)

@Serializable
data class ClientLuaEntrypointIndexEntry(
    val bundle: String,
    val path: String,
    val module: String,
    val url: String
)

@Serializable
data class ClientLuaModuleIndexEntry(
    val hash: String,
    val url: String
)

@Serializable
data class ClientLuaModuleSourceResponse(
    val module: String,
    val bundle: String,
    val path: String,
    val hash: String,
    val source: String
)

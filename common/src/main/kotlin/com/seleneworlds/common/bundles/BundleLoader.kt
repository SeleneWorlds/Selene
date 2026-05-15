package com.seleneworlds.common.bundles

import org.slf4j.Logger
import com.seleneworlds.common.script.ConstantTrace
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.util.xpCall
import com.seleneworlds.common.serialization.SerializedMap
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class BundlePreloadSpec(
    val moduleName: String,
    val file: String,
    val encoding: Charset
)

class BundleLoader(
    private val logger: Logger,
    private val luaManager: LuaManager,
    private val luaPackage: LuaPackageModule,
    private val bundleDatabase: BundleDatabase,
    private val bundleLocator: BundleLocator
) {

    fun loadBundles(bundles: Set<String>): List<Bundle> {
        val bundleManifests = mutableMapOf<String, Bundle>()
        val dependencyGraph = mutableMapOf<String, List<String>>()
        val missingBundles = mutableSetOf<String>()

        // Recursively resolve and load manifests for all required bundles
        fun collectDependencies(bundle: String) {
            if (bundleManifests.containsKey(bundle)) return
            val locatedBundle = bundleLocator.locateBundle(bundle)
            if (locatedBundle == null) {
                missingBundles.add(bundle)
                return
            }
            bundleManifests[bundle] = locatedBundle
            dependencyGraph[bundle] = locatedBundle.manifest.dependencies
            for (dependency in locatedBundle.manifest.dependencies) {
                collectDependencies(dependency)
            }
            luaPackage.addPackageResolver { path ->
                if (path == bundle) {
                    val luaInitFile =
                        File(locatedBundle.dir, "init.lua")
                    if (luaInitFile.exists()) {
                        return@addPackageResolver (locatedBundle.getFileDebugName(luaInitFile)) to luaInitFile.readText()
                    }
                }
                if (!path.startsWith("$bundle.")) {
                    return@addPackageResolver null
                }
                val luaFile =
                    File(locatedBundle.dir, path.substringAfter('.').replace('.', File.separatorChar) + ".lua")
                if (luaFile.exists()) {
                    return@addPackageResolver (locatedBundle.getFileDebugName(luaFile)) to luaFile.readText()
                }
                null
            }
        }
        for (bundle in bundles) {
            collectDependencies(bundle)
        }

        if (missingBundles.isNotEmpty()) {
            logger.error("Missing required bundles: ${missingBundles.joinToString(", ")}")
            return emptyList()
        }

        // Topological sort only on loaded/required bundles
        fun topoSort(graph: Map<String, List<String>>, nodes: Set<String>): List<String>? {
            val visited = mutableSetOf<String>()
            val temp = mutableSetOf<String>()
            val result = mutableListOf<String>()
            fun visit(node: String): Boolean {
                if (node !in nodes) return true
                if (node in visited) return true
                if (node in temp) return false // cycle
                temp.add(node)
                for (dep in graph[node].orEmpty()) {
                    if (dep in nodes && !visit(dep)) return false
                }
                temp.remove(node)
                visited.add(node)
                result.add(node)
                return true
            }
            for (node in nodes) {
                if (!visit(node)) return null
            }
            return result
        }

        val sortedBundles = topoSort(dependencyGraph, bundleManifests.keys)
        if (sortedBundles == null) {
            logger.error("Bundle dependency cycle detected. Aborting bundle load.")
            return emptyList()
        }

        for (bundle in sortedBundles) {
            val locatedBundle = bundleManifests[bundle] ?: continue
            bundleDatabase.addBundle(locatedBundle)
        }

        // Load bundles in dependency order
        for (bundle in sortedBundles) {
            val locatedBundle = bundleManifests[bundle] ?: continue
            val manifest = locatedBundle.manifest
            val bundleDir = locatedBundle.dir
            for (preloadSpec in manifest.getPreloadSpecs()) {
                try {
                    val scriptFile = File(bundleDir, preloadSpec.file)
                    if (scriptFile.exists()) {
                        logger.debug(
                            "Pre-loading Lua module {} from {} with encoding {}",
                            preloadSpec.moduleName,
                            preloadSpec.file,
                            preloadSpec.encoding
                        )
                        luaPackage.preloadModule(
                            luaManager.lua,
                            preloadSpec.moduleName,
                            scriptFile.readText(preloadSpec.encoding),
                            locatedBundle.getFileDebugName(scriptFile)
                        )
                    } else {
                        logger.error("Preload file {} not found in bundle {}", preloadSpec.file, bundle)
                    }
                } catch (e: Exception) {
                    logger.error("Error pre-loading Lua module {}: {}", preloadSpec.moduleName, e.message)
                }
            }
        }

        return sortedBundles.mapNotNull { bundleManifests[it] }
    }

    fun loadBundleEntrypoints(bundles: List<Bundle>, entrypointFilters: List<String>) {
        for (bundle in bundles) {
            val manifest = bundle.manifest
            for (entrypoint in manifest.entrypoints) {
                if (entrypointFilters.none { entrypoint.startsWith(it) }) {
                    continue
                }
                runBundleEntrypoint(bundle, entrypoint)
            }
        }
    }

    fun runBundleEntrypoint(bundle: Bundle, entrypoint: String) {
        val scriptFile = File(bundle.dir, entrypoint)
        if (scriptFile.exists()) {
            try {
                BundleExecutionContext.withBundle(bundle) {
                    val lua = luaManager.lua
                    lua.load(LuaManager.loadBuffer(scriptFile.readText()), bundle.getFileDebugName(scriptFile))
                    lua.xpCall(0, 1, ConstantTrace("[entrypoint \"$entrypoint\"] in bundle \"${bundle.manifest.name}\""))
                }
            } catch (e: Exception) {
                logger.error("Lua Error in Entrypoint", e)
            }
        } else {
            logger.error("Entrypoint $entrypoint not found in bundle $bundle")
        }
    }
}

fun BundleManifest.getPreloadSpecs(): List<BundlePreloadSpec> {
    return preloads.map { (moduleName, preload) ->
        when (preload) {
            is String -> BundlePreloadSpec(moduleName, preload, StandardCharsets.UTF_8)
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val preloadMap = preload as SerializedMap
                val file = preloadMap["file"] as? String
                    ?: throw IllegalArgumentException("Preload object must have 'file' field")
                val encoding = (preloadMap["encoding"] as? String)?.let { Charset.forName(it) }
                    ?: StandardCharsets.UTF_8
                BundlePreloadSpec(moduleName, file, encoding)
            }

            else -> throw IllegalArgumentException("Preload must be either a string or an object with 'file' and optional 'encoding' fields")
        }
    }
}

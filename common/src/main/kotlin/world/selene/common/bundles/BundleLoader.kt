package world.selene.common.bundles

import org.slf4j.Logger
import world.selene.common.lua.LuaManager
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.collections.iterator

class BundleLoader(
    private val logger: Logger,
    private val luaManager: LuaManager,
    private val bundleDatabase: BundleDatabase,
    private val bundleLocator: BundleLocator
) {

    fun readBundleFileText(bundle: LocatedBundle, file: File, encoding: Charset = StandardCharsets.UTF_8): String {
        val lua = luaManager.lua
        var textContent = file.readText(encoding)
        for ((_, transformer) in bundle.transformers) {
            transformer.push(lua)
            lua.getField(-1, "transformText")
            if (lua.isFunction(-1)) {
                lua.push(textContent)
                lua.pCall(1, 1)
                if (lua.isString(-1)) {
                    textContent = lua.toString(-1)!!
                }
            }
            lua.pop(2) // transformer and result
        }

        return textContent
    }

    fun loadBundles(bundles: Set<String>): List<LocatedBundle> {
        val bundleManifests = mutableMapOf<String, LocatedBundle>()
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
            luaManager.addPackageResolver { path ->
                if (path == bundle) {
                    val luaInitFile =
                        File(locatedBundle.dir, "init.lua")
                    if (luaInitFile.exists()) {
                        return@addPackageResolver (locatedBundle.getFileDebugName(luaInitFile)) to readBundleFileText(locatedBundle, luaInitFile)
                    }
                }
                if (!path.startsWith("$bundle.")) {
                    return@addPackageResolver null
                }
                val luaFile =
                    File(locatedBundle.dir, path.substringAfter('.').replace('.', File.separatorChar) + ".lua")
                if (luaFile.exists()) {
                    return@addPackageResolver (locatedBundle.getFileDebugName(luaFile)) to readBundleFileText(locatedBundle, luaFile)
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
            for (transformerPath in manifest.transformers) {
                val transformerFile = File(bundleDir, transformerPath)
                luaManager.runScript(locatedBundle, transformerFile, transformerFile.readText())
                val transformer = luaManager.lua.get()
                locatedBundle.transformers[transformerPath] = transformer
            }
            for ((moduleName, preload) in manifest.preloads) {
                try {
                    val (preloadFile, encoding) = when (preload) {
                        is String -> preload to StandardCharsets.UTF_8
                        is Map<*, *> -> {
                            val preloadMap = preload as Map<String, Any>
                            val file = preloadMap["file"] as? String
                                ?: throw IllegalArgumentException("Preload object must have 'file' field")
                            val enc = (preloadMap["encoding"] as? String)?.let { Charset.forName(it) }
                                ?: StandardCharsets.UTF_8
                            file to enc
                        }

                        else -> throw IllegalArgumentException("Preload must be either a string or an object with 'file' and optional 'encoding' fields")
                    }

                    val scriptFile = File(bundleDir, preloadFile)
                    if (scriptFile.exists()) {
                        logger.debug("Pre-loading Lua module $moduleName from $preloadFile with encoding $encoding")
                        luaManager.preloadModule(moduleName, readBundleFileText(locatedBundle, scriptFile, encoding))
                    } else {
                        logger.error("Preload file $preloadFile not found in bundle $bundle")
                    }
                } catch (e: Exception) {
                    logger.error("Error pre-loading Lua module $moduleName: ${e.message}")
                }
            }
        }

        return sortedBundles.mapNotNull { bundleManifests[it] }
    }

    fun loadBundleEntrypoints(bundles: List<LocatedBundle>, entrypointFilters: List<String>) {
        for (bundle in bundles) {
            val manifest = bundle.manifest
            val bundleDir = bundle.dir
            for (entrypoint in manifest.entrypoints) {
                if (entrypointFilters.none { entrypoint.startsWith(it) }) {
                    continue
                }

                val scriptFile = File(bundleDir, entrypoint)
                if (scriptFile.exists()) {
                    try {
                        luaManager.runScript(bundle, scriptFile, readBundleFileText(bundle, scriptFile))
                    } catch (e: Exception) {
                        logger.error("Error loading ${bundle.manifest.name} entrypoint $entrypoint: ${e.message}", e)
                    }
                } else {
                    logger.error("Entrypoint $entrypoint not found in bundle $bundle")
                }
            }
        }
    }
}

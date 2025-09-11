package world.selene.common.bundles

import org.slf4j.Logger
import world.selene.common.lua.ClosureTrace
import world.selene.common.lua.LuaManager
import world.selene.common.lua.libraries.LuaPackageModule
import world.selene.common.lua.util.xpCall
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

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
            for ((moduleName, preload) in manifest.preloads) {
                try {
                    val (preloadFile, encoding) = when (preload) {
                        is String -> preload to StandardCharsets.UTF_8
                        is Map<*, *> -> {
                            @Suppress("UNCHECKED_CAST")
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
                        logger.debug(
                            "Pre-loading Lua module {} from {} with encoding {}",
                            moduleName,
                            preloadFile,
                            encoding
                        )
                        luaPackage.preloadModule(luaManager.lua, moduleName, scriptFile.readText(encoding))
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

    fun loadBundleEntrypoints(bundles: List<Bundle>, entrypointFilters: List<String>) {
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
                        val lua = luaManager.lua
                        lua.load(LuaManager.loadBuffer(scriptFile.readText()), bundle.getFileDebugName(scriptFile))
                        lua.xpCall(
                            0,
                            1,
                            ClosureTrace { "[entrypoint \"${entrypoint}\"] in bundle \"${bundle.manifest.name}\"" })
                    } catch (e: Exception) {
                        logger.error("Lua Error in Entrypoint", e)
                    }
                } else {
                    logger.error("Entrypoint $entrypoint not found in bundle $bundle")
                }
            }
        }
    }
}

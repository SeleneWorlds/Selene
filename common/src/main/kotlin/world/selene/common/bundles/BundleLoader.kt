package world.selene.common.bundles

import org.slf4j.Logger
import party.iroiro.luajava.Lua
import world.selene.common.lua.LuaManager
import java.io.File
import kotlin.collections.iterator

class BundleLoader(
    private val logger: Logger,
    private val luaManager: LuaManager,
    private val bundleDatabase: BundleDatabase,
    private val bundleLocator: BundleLocator
) {

    fun readBundleFileText(bundle: LocatedBundle, file: File): String {
        var byteContent = file.readBytes()
        val lua = luaManager.lua
        for ((_, transformer) in bundle.transformers) {
            transformer.push(lua)
            lua.getField(-1, "transformBytes")
            if (lua.isFunction(-1)) {
                lua.push(byteContent, Lua.Conversion.FULL)
                lua.pCall(1, 1)
                if (lua.isTable(-1)) {
                    byteContent = lua.toList(-1)!!.map { (it as Double).toInt().toByte() }.toByteArray()
                }
            }
            lua.pop(2) // transformer and result
        }
        return String(byteContent, Charsets.UTF_8)
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
            luaManager.addPackageResolver { lua, path ->
                if (path == bundle) {
                    val luaInitFile =
                        File(locatedBundle.dir, "init.lua")
                    if (luaInitFile.exists()) {
                        val top = lua.top
                        lua.run(readBundleFileText(locatedBundle, luaInitFile))
                        return@addPackageResolver if (lua.top > top) lua.get() else null
                    }
                }
                if (!path.startsWith("$bundle.")) {
                    return@addPackageResolver null
                }
                val luaFile =
                    File(locatedBundle.dir, path.substringAfter('.').replace('.', File.separatorChar) + ".lua")
                if (luaFile.exists()) {
                    val top = lua.top
                    lua.run(readBundleFileText(locatedBundle, luaFile))
                    return@addPackageResolver if (lua.top > top) lua.get() else null
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
            for (transformerFile in manifest.transformers) {
                luaManager.runScript(File(bundleDir, transformerFile).readText())
                val transformer = luaManager.lua.get()
                locatedBundle.transformers[transformerFile] = transformer
            }
            for ((moduleName, preloadFile) in manifest.preloads) {
                val scriptFile = File(bundleDir, preloadFile)
                if (scriptFile.exists()) {
                    try {
                        logger.debug("Pre-loading Lua module $moduleName from $preloadFile")
                        luaManager.preloadModule(moduleName, readBundleFileText(locatedBundle, scriptFile))
                    } catch (e: Exception) {
                        logger.error("Error pre-loading Lua module $preloadFile: ${e.message}")
                    }
                } else {
                    logger.error("Preload file $preloadFile not found in bundle $bundle")
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
                        luaManager.runScript(readBundleFileText(bundle, scriptFile))
                    } catch (e: Exception) {
                        logger.error("Error loading Lua entrypoint $entrypoint: ${e.message}", e)
                    }
                } else {
                    logger.error("Entrypoint $entrypoint not found in bundle $bundle")
                }
            }
        }
    }
}

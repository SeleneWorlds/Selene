package com.seleneworlds.server.script

import org.slf4j.Logger
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleLoader
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.bundles.getPreloadSpecs
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import java.io.File

class ServerScriptHotReload(
    private val bundleLoader: BundleLoader,
    private val bundleDatabase: BundleDatabase,
    private val luaManager: LuaManager,
    private val luaPackage: LuaPackageModule,
    private val logger: Logger
) {

    fun reloadBundleClosure(bundleId: String, deletedFiles: Set<String>) {
        val impactedBundleIds = bundleDatabase.getTransitiveDependents(bundleId) + bundleId
        val bundlesToReload = bundleDatabase.loadedBundles.filter { it.manifest.name in impactedBundleIds }
        if (bundlesToReload.isEmpty()) {
            logger.warn("Could not eager reload unknown bundle {}", bundleId)
            return
        }

        bundlesToReload.forEach { bundle ->
            clearBundleState(bundle, if (bundle.manifest.name == bundleId) deletedFiles else emptySet())
        }
        bundlesToReload.forEach { bundle ->
            preloadBundleModules(bundle)
        }
        bundlesToReload.forEach { bundle ->
            rerunBundleEntrypoints(bundle)
        }

        logger.info(
            "Eager reloaded bundle closure for {}: {}",
            bundleId,
            bundlesToReload.joinToString(", ") { it.manifest.name }
        )
    }

    fun reloadUpdatedScripts(bundle: Bundle, updatedFiles: Set<String>) {
        updatedFiles.forEach { relativePath ->
            reloadUpdatedScript(bundle, relativePath)
        }
    }

    fun unloadDeletedScripts(bundle: Bundle, deletedFiles: Set<String>) {
        deletedFiles.forEach { relativePath ->
            unloadDeletedScript(bundle, relativePath)
        }
    }

    private fun reloadUpdatedScript(bundle: Bundle, relativePath: String) {
        val normalizedPath = relativePath.replace('\\', '/')
        if (!normalizedPath.startsWith("server/") && !normalizedPath.startsWith("common/") && normalizedPath != "init.lua") {
            return
        }

        val scriptFile = bundle.dir.resolve(relativePath)
        if (!scriptFile.isFile) {
            return
        }

        var reloaded = false
        for (preloadSpec in bundle.manifest.getPreloadSpecs()) {
            if (preloadSpec.file.replace('\\', '/') != normalizedPath) {
                continue
            }

            luaPackage.preloadModule(
                luaManager.lua,
                preloadSpec.moduleName,
                scriptFile.readText(preloadSpec.encoding),
                bundle.getFileDebugName(scriptFile)
            )
            luaPackage.clearLoadedModule(luaManager.lua, preloadSpec.moduleName)
            logger.info("Reloaded server Lua module {} from {}", preloadSpec.moduleName, normalizedPath)
            reloaded = true
        }

        val resolverModuleName = bundle.moduleNameForLuaFile(normalizedPath)
        if (resolverModuleName != null) {
            luaPackage.clearLoadedModule(luaManager.lua, resolverModuleName)
            logger.info("Invalidated server Lua module {} from {}", resolverModuleName, normalizedPath)
            reloaded = true
        }

        if (normalizedPath in bundle.manifest.entrypoints && serverEntrypointFilters.any { normalizedPath.startsWith(it) }) {
            bundleLoader.runBundleEntrypoint(bundle, normalizedPath)
            logger.info("Re-ran hot reloaded bundle entrypoint {} from {}", normalizedPath, bundle.manifest.name)
            reloaded = true
        }

        if (!reloaded) {
            logger.debug("No server Lua module mapping found for {}", normalizedPath)
        }
    }

    private fun unloadDeletedScript(bundle: Bundle, relativePath: String) {
        val normalizedPath = relativePath.replace('\\', '/')
        if (!normalizedPath.startsWith("server/") && !normalizedPath.startsWith("common/") && normalizedPath != "init.lua") {
            return
        }

        var unloaded = false
        for (preloadSpec in bundle.manifest.getPreloadSpecs()) {
            if (preloadSpec.file.replace('\\', '/') != normalizedPath) {
                continue
            }

            luaPackage.clearLoadedModule(luaManager.lua, preloadSpec.moduleName)
            luaPackage.removePreloadedModule(luaManager.lua, preloadSpec.moduleName)
            logger.info("Unloaded deleted server Lua module {} from {}", preloadSpec.moduleName, normalizedPath)
            unloaded = true
        }

        val resolverModuleName = bundle.moduleNameForLuaFile(normalizedPath)
        if (resolverModuleName != null) {
            luaPackage.clearLoadedModule(luaManager.lua, resolverModuleName)
            logger.info("Invalidated deleted server Lua module {} from {}", resolverModuleName, normalizedPath)
            unloaded = true
        }

        if (!unloaded) {
            logger.debug("No server Lua module mapping found for deleted file {}", normalizedPath)
        }
    }

    private fun Bundle.moduleNameForLuaFile(relativePath: String): String? {
        if (!relativePath.endsWith(".lua")) {
            return null
        }

        if (relativePath == "init.lua") {
            return manifest.name
        }

        val modulePath = relativePath.removeSuffix(".lua").replace('/', '.')
        return "${manifest.name}.$modulePath"
    }

    private fun clearBundleState(bundle: Bundle, deletedFiles: Set<String>) {
        val moduleNames = bundle.listLuaModuleNames() + deletedFiles.mapNotNull { bundle.moduleNameForLuaFile(it.replace('\\', '/')) }
        for (moduleName in moduleNames) {
            luaPackage.clearLoadedModule(luaManager.lua, moduleName)
        }
        for (preloadSpec in bundle.manifest.getPreloadSpecs()) {
            luaPackage.clearLoadedModule(luaManager.lua, preloadSpec.moduleName)
            luaPackage.removePreloadedModule(luaManager.lua, preloadSpec.moduleName)
        }
    }

    private fun preloadBundleModules(bundle: Bundle) {
        for (preloadSpec in bundle.manifest.getPreloadSpecs()) {
            val scriptFile = File(bundle.dir, preloadSpec.file)
            if (!scriptFile.isFile) {
                logger.debug("Skipping missing eager preload {} in bundle {}", preloadSpec.file, bundle.manifest.name)
                continue
            }

            luaPackage.preloadModule(
                luaManager.lua,
                preloadSpec.moduleName,
                scriptFile.readText(preloadSpec.encoding),
                bundle.getFileDebugName(scriptFile)
            )
        }
    }

    private fun rerunBundleEntrypoints(bundle: Bundle) {
        bundle.manifest.entrypoints
            .filter { entrypoint -> serverEntrypointFilters.any { entrypoint.startsWith(it) } }
            .forEach { entrypoint ->
                bundleLoader.runBundleEntrypoint(bundle, entrypoint)
                logger.info("Re-ran eager hot reload bundle entrypoint {} from {}", entrypoint, bundle.manifest.name)
            }
    }

    private fun Bundle.listLuaModuleNames(): Set<String> {
        val moduleNames = mutableSetOf<String>()

        dir.walkTopDown()
            .filter { it.isFile && it.extension == "lua" }
            .forEach { file ->
                val relativePath = file.relativeTo(dir).invariantSeparatorsPath
                moduleNameForLuaFile(relativePath)?.let(moduleNames::add)
            }

        manifest.getPreloadSpecs()
            .mapTo(moduleNames) { it.moduleName }

        return moduleNames
    }

    companion object {
        private val serverEntrypointFilters = listOf("common/", "server/", "init.lua")
    }
}

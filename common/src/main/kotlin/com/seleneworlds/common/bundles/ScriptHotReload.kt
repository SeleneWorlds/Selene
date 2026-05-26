package com.seleneworlds.common.bundles

import org.slf4j.Logger
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule

class ScriptHotReload(
    private val bundleLifecycleManager: BundleLifecycleManager,
    private val bundleLoader: BundleLoader,
    private val luaManager: LuaManager,
    private val luaPackage: LuaPackageModule,
    private val logger: Logger,
    private val scriptRoots: Set<String>,
    private val entrypointFilters: List<String>
) {

    fun reloadBundleClosure(bundleId: String, deletedFiles: Set<String>) {
        bundleLifecycleManager.reloadBundleClosure(bundleId, deletedFiles)
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
        val normalizedPath = normalizePath(relativePath)
        if (!isTrackedScriptPath(normalizedPath)) {
            return
        }

        val scriptFile = bundle.dir.resolve(relativePath)
        if (!scriptFile.isFile) {
            return
        }

        var reloaded = false
        for (preloadSpec in bundle.manifest.getPreloadSpecs()) {
            if (normalizePath(preloadSpec.file) != normalizedPath) {
                continue
            }

            luaPackage.preloadModule(
                luaManager.lua,
                preloadSpec.moduleName,
                scriptFile.readText(preloadSpec.encoding),
                bundle.getFileDebugName(scriptFile)
            )
            luaPackage.clearLoadedModule(luaManager.lua, preloadSpec.moduleName)
            logger.info("Reloaded Lua module {} from {}", preloadSpec.moduleName, normalizedPath)
            reloaded = true
        }

        val resolverModuleName = bundleLoader.moduleNameForLuaFile(bundle, normalizedPath)
        if (resolverModuleName != null) {
            luaPackage.clearLoadedModule(luaManager.lua, resolverModuleName)
            logger.info("Invalidated Lua module {} from {}", resolverModuleName, normalizedPath)
            reloaded = true
        }

        if (normalizedPath in bundle.manifest.entrypoints && entrypointFilters.any { normalizedPath.startsWith(it) }) {
            bundleLoader.runBundleEntrypoint(bundle, normalizedPath)
            logger.info("Re-ran hot reloaded bundle entrypoint {} from {}", normalizedPath, bundle.manifest.name)
            reloaded = true
        }

        if (!reloaded) {
            logger.debug("No Lua module mapping found for {}", normalizedPath)
        }
    }

    private fun unloadDeletedScript(bundle: Bundle, relativePath: String) {
        val normalizedPath = normalizePath(relativePath)
        if (!isTrackedScriptPath(normalizedPath)) {
            return
        }

        var unloaded = false
        for (preloadSpec in bundle.manifest.getPreloadSpecs()) {
            if (normalizePath(preloadSpec.file) != normalizedPath) {
                continue
            }

            luaPackage.clearLoadedModule(luaManager.lua, preloadSpec.moduleName)
            luaPackage.removePreloadedModule(luaManager.lua, preloadSpec.moduleName)
            logger.info("Unloaded deleted Lua module {} from {}", preloadSpec.moduleName, normalizedPath)
            unloaded = true
        }

        val resolverModuleName = bundleLoader.moduleNameForLuaFile(bundle, normalizedPath)
        if (resolverModuleName != null) {
            luaPackage.clearLoadedModule(luaManager.lua, resolverModuleName)
            logger.info("Invalidated deleted Lua module {} from {}", resolverModuleName, normalizedPath)
            unloaded = true
        }

        if (!unloaded) {
            logger.debug("No Lua module mapping found for deleted file {}", normalizedPath)
        }
    }

    private fun isTrackedScriptPath(path: String): Boolean {
        return path == "init.lua" || scriptRoots.any { path.startsWith("$it/") }
    }

    private fun normalizePath(path: String): String = path.replace('\\', '/')
}

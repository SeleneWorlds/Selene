package com.seleneworlds.server.script

import org.slf4j.Logger
import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleLifecycleManager
import com.seleneworlds.common.bundles.BundleLoader
import com.seleneworlds.common.bundles.getPreloadSpecs
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import java.io.File

class ServerScriptHotReload(
    private val bundleLifecycleManager: BundleLifecycleManager,
    private val bundleLoader: BundleLoader,
    private val luaManager: LuaManager,
    private val luaPackage: LuaPackageModule,
    private val logger: Logger
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

        val resolverModuleName = bundleLoader.moduleNameForLuaFile(bundle, normalizedPath)
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

        val resolverModuleName = bundleLoader.moduleNameForLuaFile(bundle, normalizedPath)
        if (resolverModuleName != null) {
            luaPackage.clearLoadedModule(luaManager.lua, resolverModuleName)
            logger.info("Invalidated deleted server Lua module {} from {}", resolverModuleName, normalizedPath)
            unloaded = true
        }

        if (!unloaded) {
            logger.debug("No server Lua module mapping found for deleted file {}", normalizedPath)
        }
    }

    companion object {
        private val serverEntrypointFilters = listOf("common/", "server/", "init.lua")
    }
}

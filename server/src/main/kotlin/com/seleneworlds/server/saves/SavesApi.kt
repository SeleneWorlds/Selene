package com.seleneworlds.server.saves

import com.seleneworlds.common.serialization.SerializedMap
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.server.maps.tree.MapTreeApi
import com.seleneworlds.server.maps.layers.MapLayerApi

class SavesApi(val saveManager: SaveManager, val serverConfig: ServerConfig) {

    /**
     * Checks if a save file exists at the specified path.
     *
     * ```signatures
     * Has(path: string) -> boolean
     * ```
     */
    fun has(path: String): Boolean {
        val saveFile = serverConfig.resolveSavePath(path)
        return saveFile.exists()
    }

    /**
     * Saves an object to a file at the specified path.
     *
     * ```signatures
     * Save(object: any, path: string)
     * ```
     */
    fun save(savable: Any, path: String) {
        val saveFile = serverConfig.resolveSavePath(path)
        val value = when (savable) {
            is MapTreeApi -> savable.mapTree
            else -> savable
        }
        saveManager.save(saveFile, value)
    }

    /** Loads a saved map tree. Rejects standalone layer saves. */
    fun loadMapTree(path: String): MapTreeApi =
        saveManager.loadMapTree(serverConfig.resolveSavePath(path)).api

    /** Loads a standalone layer that can be attached with map:addLayer. */
    fun loadMapLayer(path: String): MapLayerApi =
        saveManager.loadMapLayer(serverConfig.resolveSavePath(path))

    fun loadKeyValueMap(path: String): SerializedMap {
        val saveFile = serverConfig.resolveSavePath(path)
        return saveManager.loadKeyValueMap(saveFile)
    }
}

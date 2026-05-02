package com.seleneworlds.server.saves

import com.seleneworlds.common.script.ExposedApi
import com.seleneworlds.server.config.ServerConfig
import java.io.File

class SavesApi(val saveManager: SaveManager, val serverConfig: ServerConfig) {

    /**
     * Checks if a save file exists at the specified path.
     *
     * ```signatures
     * Has(path: string) -> boolean
     * ```
     */
    fun has(path: String): Boolean {
        val saveFile = File(serverConfig.savePath, path)
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
        val saveFile = File(serverConfig.savePath, path)
        saveManager.save(saveFile, savable)
    }

    /**
     * Loads an object from a file at the specified path.
     *
     * ```signatures
     * Load(path: string) -> any
     * ```
     */
    fun load(path: String): Any? {
        val saveFile = File(serverConfig.savePath, path)
        val loaded = saveManager.load(saveFile)
        if (loaded is ExposedApi<*>) {
            return loaded.api
        }
        return null
    }
}

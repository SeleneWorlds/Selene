package com.seleneworlds.server.config

import com.seleneworlds.common.config.HotReloadMode
import java.io.File

data class ServerConfig(
    val name: String = "New Server",
    val port: Int = 8147,
    val webSocketPort: Int = 8148,
    val maxQueuedPacketsPerClient: Int = 256,
    val maxPacketsPerClientPerTick: Int = 64,
    val apiPort: Int = 8080,
    val savePath: String = "save",
    val bundlesPath: String = "bundles",
    val bundles: List<String> = emptyList(),
    val insecureMode: Boolean = false,
    val public: Boolean = false,
    val announcedHost: String = "",
    val announcedApi: String = "",
    val announcedWebSocket: String = "",
    val proxyHeaders: String = "none",
    val apiCorsOrigins: List<String> = emptyList(),
    val hotReload: String = "false",
    val grid: String = ""
) {
    val hotReloadMode: HotReloadMode
        get() = HotReloadMode.parse(hotReload)

    /** Resolves a user-supplied path without allowing it to escape the configured save directory. */
    fun resolveSavePath(path: String): File {
        require(!File(path).isAbsolute) { "Save path must be relative: $path" }

        val saveRoot = File(savePath).canonicalFile
        val resolved = File(saveRoot, path).canonicalFile
        require(resolved.toPath().startsWith(saveRoot.toPath())) {
            "Save path escapes the save directory: $path"
        }
        return resolved
    }

    companion object {
        fun createDefault() {
            val configFile = File("server.properties")
            if (!configFile.exists()) {
                configFile.writer().use { output ->
                    ServerConfig::class.java.classLoader.getResourceAsStream("default-server.properties")
                        ?.bufferedReader()
                        .use { input ->
                            input?.copyTo(output)
                        } ?: throw IllegalStateException("Failed to read default server properties")
                }
            }
        }
    }
}

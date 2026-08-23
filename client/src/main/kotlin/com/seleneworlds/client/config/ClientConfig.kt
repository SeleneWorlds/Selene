package com.seleneworlds.client.config

import com.seleneworlds.common.config.HotReloadMode
import java.io.File

data class ClientConfig(
    val vsync: Boolean = true,
    val hotReload: String = "true",
    val browserUiEnabled: Boolean = true,
    val browserUiUrl: String = "",
    val browserUiInsecure: Boolean = false,
    val browserUiStorageDir: String = "ui-data",
    val cefInstallDir: String = "jcef-bundle"
) {
    val hotReloadMode: HotReloadMode
        get() = HotReloadMode.parse(hotReload)

    companion object {
        fun createDefault() {
            val configFile = File("client.properties")
            if (!configFile.exists()) {
                configFile.writer().use { output ->
                    ClientConfig::class.java.classLoader.getResourceAsStream("default-client.properties")
                        ?.bufferedReader()
                        .use { input ->
                            input?.copyTo(output)
                        } ?: throw IllegalStateException("Failed to read default client properties")
                }
            }
        }
    }
}

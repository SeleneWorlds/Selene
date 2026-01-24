package world.selene.client.config

import java.io.File

data class ClientConfig(val vsync: Boolean = true, val hotReload: Boolean = true) {
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

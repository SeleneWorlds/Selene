package world.selene.server.config

import java.io.File

data class ServerConfig(
    val name: String = "New Server",
    val port: Int = 8147,
    val managementPort: Int = 8080,
    val savePath: String = "save",
    val bundlesPath: String = "bundles",
    val bundles: List<String> = emptyList(),
    val insecureMode: Boolean = false,
    val heartbeatServer: String = "https://telescope.seleneworlds.com"
) {
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

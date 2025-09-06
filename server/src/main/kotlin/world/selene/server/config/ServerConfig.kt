package world.selene.server.config

import java.io.File

data class ServerConfig(
    val port: Int,
    val savePath: String,
    val bundlesPath: String,
    val bundles: List<String>,
    val insecureMode: Boolean
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

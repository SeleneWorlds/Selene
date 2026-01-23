package world.selene.client

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.ExperimentalHoplite
import world.selene.client.config.ClientConfig
import world.selene.client.config.ClientRuntimeConfig

val objectMapper = JsonMapper.builder()
    .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).build()
    .registerKotlinModule()

@OptIn(ExperimentalHoplite::class)
fun main(args: Array<String>) {
    ClientConfig.createDefault()
    val config = ConfigLoaderBuilder.default()
        .withExplicitSealedTypes()
        .build()
        .loadConfigOrThrow<ClientConfig>("client.properties")
    val runtimeConfig = parseClientRuntimeConfig(args)

    val appConfig = Lwjgl3ApplicationConfiguration()
    appConfig.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 2)
    appConfig.setBackBufferConfig(8, 8, 8, 8, 24, 0, 4)
    appConfig.setPauseWhenMinimized(false)
    appConfig.useVsync(config.vsync)
    appConfig.setTitle("Selene")
    appConfig.setWindowedMode(1024, 768)
    appConfig.setWindowIcon("icon_16.png", "icon_32.png", "icon_128.png")
    Lwjgl3Application(SeleneApplication(config, runtimeConfig), appConfig)
}

fun parseClientRuntimeConfig(args: Array<String>): ClientRuntimeConfig {
    var host = "localhost"
    var port = 8147
    var token = ""
    var contentServerUrl = ""
    val bundles = mutableMapOf<String, String>()

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "--host", "-h" -> {
                if (i + 1 < args.size) {
                    host = args[i + 1]
                    i++
                } else {
                    throw IllegalArgumentException("Missing value for ${args[i]}")
                }
            }

            "--port", "-p" -> {
                if (i + 1 < args.size) {
                    port = args[i + 1].toIntOrNull()
                        ?: throw IllegalArgumentException("Invalid port number: ${args[i + 1]}")
                    i++
                } else {
                    throw IllegalArgumentException("Missing value for ${args[i]}")
                }
            }

            "--contentServer", "-c" -> {
                if (i + 1 < args.size) {
                    contentServerUrl = args[i + 1]
                    i++
                } else {
                    throw IllegalArgumentException("Missing value for ${args[i]}")
                }
            }

            "--token", "-t" -> {
                if (i + 1 < args.size) {
                    token = args[i + 1]
                    i++
                } else {
                    throw IllegalArgumentException("Missing value for ${args[i]}")
                }
            }

            "--bundle", "-b" -> {
                if (i + 2 < args.size) {
                    val bundleName = args[i + 1]
                    val bundlePath = args[i + 2]
                    bundles[bundleName] = bundlePath
                    i += 2
                } else {
                    throw IllegalArgumentException("Bundle requires both name and path: --bundle <name> <path>")
                }
            }

            "--help" -> {
                printUsage()
                kotlin.system.exitProcess(0)
            }

            else -> {
                if (args[i].startsWith("-")) {
                    throw IllegalArgumentException("Unknown argument: ${args[i]}")
                }
                // Ignore non-flag arguments
            }
        }
        i++
    }

    return ClientRuntimeConfig(
        host = host,
        port = port,
        contentServerUrl = contentServerUrl,
        token = token,
        bundles = bundles
    )
}

fun printUsage() {
    println(
        """
        Selene Client Usage:
        
        Options:
          --host, -h <hostname>                      Server hostname (default: localhost)
          --port, -p <port>                          Server port (default: 8147)
          --contentServer, -c <contentServerUrl>     Content server URL (default: empty)
          --token, -t <token>                        Authentication token (default: empty)
          --bundle, -b <name> <path>                 Add bundle mapping (can be used multiple times)
          --help                                     Show this help message
          
        Examples:
          java -jar selene-client.jar --host example.com --port 9000 --token eyJhb...
          java -jar selene-client.jar --bundle core /path/to/core --bundle ui /path/to/ui
    """.trimIndent()
    )
}
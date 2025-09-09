package world.selene.server.config

import java.io.File
import java.util.*

class ScriptProperties {
    private val properties = Properties()

    init {
        loadProperties()
    }

    private fun loadProperties() {
        val propertiesFile = File("script.properties")
        if (propertiesFile.exists()) {
            propertiesFile.inputStream().use { input ->
                properties.load(input)
            }
        }
    }

    fun getProperty(key: String): String? {
        val upperCamelCaseKey = key.toUpperCamelCase()
        val envValue = System.getenv("SELENE_SCRIPT_$upperCamelCaseKey")
        if (envValue != null) {
            return envValue
        }
        return properties.getProperty(key)
    }

    private fun String.toUpperCamelCase(): String {
        return this
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .replace(".", "_")
            .replace("-", "_")
            .uppercase()
    }
}

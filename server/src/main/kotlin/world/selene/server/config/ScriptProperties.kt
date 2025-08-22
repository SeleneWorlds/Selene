package world.selene.server.config

import java.io.File
import java.util.Properties

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
        return properties.getProperty(key)
    }
}

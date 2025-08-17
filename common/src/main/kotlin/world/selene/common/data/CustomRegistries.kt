package world.selene.common.data

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.bundles.BundleDatabase

class CustomRegistries(
    private val objectMapper: ObjectMapper
) : JsonRegistry<RegistryDefinition>(
    objectMapper,
    "common",
    "registries",
    RegistryDefinition::class
) {
    
    private val customRegistries: MutableMap<String, CustomRegistry> = mutableMapOf()

    fun loadCustomRegistries(bundleDatabase: BundleDatabase, platform: String) {
        customRegistries.clear()
        
        val definitions = entries.filterValues { it.platform == platform }
        for ((name, definition) in definitions) {
            val dynamicRegistry = CustomRegistry(objectMapper, definition)
            dynamicRegistry.load(bundleDatabase)
            customRegistries[name] = dynamicRegistry
        }
    }
    
    fun getCustomRegistry(name: String): Registry<*>? {
        return customRegistries[name]
    }
}

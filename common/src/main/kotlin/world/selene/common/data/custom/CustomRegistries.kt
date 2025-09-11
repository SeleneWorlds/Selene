package world.selene.common.data.custom

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.bundles.BundleDatabase
import world.selene.common.data.json.JsonRegistry
import world.selene.common.data.Registry
import kotlin.collections.iterator

class CustomRegistries(
    private val objectMapper: ObjectMapper
) : JsonRegistry<CustomRegistryDefinition>(
    objectMapper,
    "common",
    "registries",
    CustomRegistryDefinition::class
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

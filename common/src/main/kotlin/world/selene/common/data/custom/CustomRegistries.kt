package world.selene.common.data.custom

import com.fasterxml.jackson.databind.ObjectMapper
import world.selene.common.bundles.BundleDatabase
import world.selene.common.data.Identifier
import world.selene.common.data.Registry
import world.selene.common.data.json.FileBasedRegistry
import kotlin.collections.iterator

class CustomRegistries(
    objectMapper: ObjectMapper
) : FileBasedRegistry<CustomRegistryDefinition>(
    objectMapper,
    "common",
    "registries",
    CustomRegistryDefinition::class
) {

    private val customRegistries: MutableMap<Identifier, CustomRegistry> = mutableMapOf()

    fun loadCustomRegistries(bundleDatabase: BundleDatabase, platform: String) {
        customRegistries.clear()

        val definitions = entries.filterValues { it.platform == platform }
        for ((identifier, definition) in definitions) {
            val dynamicRegistry = CustomRegistry(objectMapper, definition)
            dynamicRegistry.load(bundleDatabase)
            customRegistries[identifier] = dynamicRegistry
        }
    }

    fun getCustomRegistry(identifier: Identifier): Registry<*>? {
        return customRegistries[identifier]
    }

    fun findByRegistryName(name: String): CustomRegistry? {
        return customRegistries.values.find { it.name == name }
    }

    companion object {
        val IDENTIFIER = Identifier.withDefaultNamespace("registries")
    }
}

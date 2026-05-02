package com.seleneworlds.common.data.custom

import kotlinx.serialization.json.Json
import com.seleneworlds.common.bundles.BundleDatabase
import com.seleneworlds.common.data.Identifier
import com.seleneworlds.common.data.Registry
import com.seleneworlds.common.data.json.FileBasedRegistry
import kotlin.collections.iterator

class CustomRegistries(
    json: Json
) : FileBasedRegistry<CustomRegistryDefinition>(
    json,
    "common",
    "registries",
    CustomRegistryDefinition::class,
    CustomRegistryDefinition.serializer()
) {

    private val customRegistries: MutableMap<Identifier, CustomRegistry> = mutableMapOf()

    fun loadCustomRegistries(bundleDatabase: BundleDatabase, platform: String) {
        customRegistries.clear()

        val definitions = entries.filterValues { it.platform == platform }
        for ((identifier, definition) in definitions) {
            val dynamicRegistry = CustomRegistry(json, definition)
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

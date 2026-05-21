package com.seleneworlds.common.data

import com.seleneworlds.common.data.json.FileBasedRegistry
import com.seleneworlds.common.serialization.toJsonElement

class RegistriesApi(
    private val registryProvider: RegistryProvider
) {

    fun findAll(registryName: String): Map<Identifier, *> {
        val registry = registryProvider.getRegistry(Identifier.parse(registryName))
            ?: throw IllegalArgumentException("Unknown registry: $registryName")
        return registry.getAll()
    }

    fun findByMetadata(registryName: String, key: String, value: Any): Any? {
        val registry = registryProvider.getRegistry(Identifier.parse(registryName))
            ?: throw IllegalArgumentException("Unknown registry: $registryName")
        return registry.findByMetadata(key, value)?.second
    }

    fun findByName(registryName: String, name: String): Any? {
        val registry = registryProvider.getRegistry(Identifier.parse(registryName))
            ?: throw IllegalArgumentException("Unknown registry: $registryName")
        return registry.get(Identifier.parse(name))
    }

    fun add(registryName: String, name: String, data: Any?): Any? {
        val registry = registryProvider.getRegistry(Identifier.parse(registryName))
            ?: throw IllegalArgumentException("Unknown registry: $registryName")
        val fileBasedRegistry = registry as? FileBasedRegistry<*>
            ?: throw IllegalArgumentException("Registry ${registry.name} does not support Lua mutation")
        val identifier = Identifier.parse(name)
        fileBasedRegistry.upsertEntry(identifier, data.toJsonElement())
        return fileBasedRegistry.get(identifier)
    }
}
